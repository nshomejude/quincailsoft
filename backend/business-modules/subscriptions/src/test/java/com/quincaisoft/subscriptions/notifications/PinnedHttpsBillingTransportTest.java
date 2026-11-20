package com.quincaisoft.subscriptions.notifications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PinnedHttpsBillingTransportTest {
 @TempDir Path temp;
 @Test void realLocalTlsAuthenticatesPinnedHostSignsEnvelopeAndNeverFollowsRedirects()throws Exception{
  Path store=temp.resolve("synthetic-test.p12");String password="synthetic-test-only";
  var keytool=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","keytool").toString(),"-genkeypair","-alias","test","-keyalg","RSA","-keysize","2048","-dname","CN=localhost","-ext","SAN=dns:localhost","-validity","2","-storetype","PKCS12","-storepass",password,"-keypass",password,"-keystore",store.toString()).redirectErrorStream(true).start();
  assertTrue(keytool.waitFor(30,TimeUnit.SECONDS));assertEquals(0,keytool.exitValue(),new String(keytool.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
  var keys=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(store)){keys.load(in,password.toCharArray());}
  var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,password.toCharArray());var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(keys);
  var context=SSLContext.getInstance("TLS");context.init(km.getKeyManagers(),tm.getTrustManagers(),new SecureRandom());
  var server=HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);server.setHttpsConfigurator(new HttpsConfigurator(context));
  byte[] secret=new byte[32];Arrays.fill(secret,(byte)7);String encoded=Base64.getEncoder().encodeToString(secret);AtomicInteger calls=new AtomicInteger();Set<String> accepted=ConcurrentHashMap.newKeySet();var errors=new ConcurrentLinkedQueue<String>();
  server.createContext("/events",exchange->{try{calls.incrementAndGet();String body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);String timestamp=exchange.getRequestHeaders().getFirst("X-Quincai-Timestamp");Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));String expected="v1="+HexFormat.of().formatHex(mac.doFinal((timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));if(!expected.equals(exchange.getRequestHeaders().getFirst("X-Quincai-Signature")))errors.add("Signature mismatch");accepted.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));exchange.sendResponseHeaders(202,-1);}catch(Exception failure){errors.add(failure.getClass().getSimpleName());}finally{exchange.close();}});
  server.createContext("/redirect",exchange->{calls.incrementAndGet();exchange.getResponseHeaders().add("Location","https://127.0.0.1/internal");exchange.sendResponseHeaders(302,-1);exchange.close();});server.start();
  try{
   String base="https://localhost:"+server.getAddress().getPort();var transport=new PinnedHttpsBillingTransport(base+"/events",Set.of("localhost"),encoded,context.getSocketFactory(),host->new InetAddress[]{InetAddress.getLoopbackAddress()});UUID event=UUID.randomUUID();var envelope=new BillingNotificationTransport.Envelope(event,"{\"schemaVersion\":1,\"eventId\":\""+event+"\"}");
   assertEquals(202,transport.send(envelope).statusCode());assertEquals(202,transport.send(envelope).statusCode());assertEquals(2,calls.get());assertEquals(1,accepted.size(),"Stable idempotency identity survives redelivery");assertTrue(errors.isEmpty(),errors.toString());
   var redirect=new PinnedHttpsBillingTransport(base+"/redirect",Set.of("localhost"),encoded,context.getSocketFactory(),host->new InetAddress[]{InetAddress.getLoopbackAddress()});assertEquals(302,redirect.send(envelope).statusCode());assertEquals(3,calls.get(),"No second request to redirect target");
   var wrongHost=new PinnedHttpsBillingTransport("https://wrong.example:"+server.getAddress().getPort()+"/events",Set.of("wrong.example"),encoded,context.getSocketFactory(),host->new InetAddress[]{InetAddress.getLoopbackAddress()});assertThrows(SSLHandshakeException.class,()->wrongHost.send(envelope));
   assertThrows(IllegalArgumentException.class,()->transport.send(new BillingNotificationTransport.Envelope(event,"x".repeat(4097))));
  }finally{server.stop(0);}
  assertThrows(IllegalArgumentException.class,()->new PinnedHttpsBillingTransport("http://localhost/events",Set.of("localhost"),encoded));
  assertThrows(IllegalArgumentException.class,()->new PinnedHttpsBillingTransport("https://127.0.0.1/events",Set.of("127.0.0.1"),encoded));
  assertThrows(IllegalArgumentException.class,()->new PinnedHttpsBillingTransport("https://localhost/events",Set.of("localhost"),encoded));
  assertThrows(IllegalArgumentException.class,()->new PinnedHttpsBillingTransport("https://localhost/events?token=secret",Set.of("localhost"),encoded));
  assertThrows(IllegalArgumentException.class,()->new PinnedHttpsBillingTransport("https://localhost/events",Set.of("other.example"),encoded));
 }
}
