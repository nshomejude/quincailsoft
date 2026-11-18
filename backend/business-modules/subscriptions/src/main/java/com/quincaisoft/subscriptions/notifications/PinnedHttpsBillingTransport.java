package com.quincaisoft.subscriptions.notifications;

import javax.net.ssl.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Connects directly to a vetted address, then authenticates the configured TLS
 * hostname. No redirects, proxy inheritance, arbitrary tenant destinations or
 * DNS re-resolution between address validation and connect. */
public final class PinnedHttpsBillingTransport implements BillingNotificationTransport {
    private static final ScheduledExecutorService DEADLINES=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("billing-webhook-deadline").factory());
    interface Resolver { InetAddress[] resolve(String host)throws IOException; }
    private final URI uri;private final String host;private final int port;private final byte[] key;
    private final SSLSocketFactory factory;private final InetAddress[] addresses;private final String destinationKey;
    public PinnedHttpsBillingTransport(String endpoint,Set<String> allowedHosts,String signingKeyBase64){
        this(endpoint,allowedHosts,signingKeyBase64,(SSLSocketFactory)SSLSocketFactory.getDefault(),PinnedHttpsBillingTransport::publicAddresses);
    }
    // Explicit local TLS test seam, unavailable through deployment properties.
    PinnedHttpsBillingTransport(String endpoint,Set<String> allowedHosts,String signingKeyBase64,SSLSocketFactory factory,Resolver resolver){
        this.uri=URI.create(endpoint);this.host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);
        if(!"https".equalsIgnoreCase(uri.getScheme())||host.isBlank()||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null||uri.getRawQuery()!=null
            ||host.indexOf(':')>=0||host.matches("[0-9.]+")||!allowedHosts.contains(host))throw new IllegalArgumentException("Exact configured allowlisted HTTPS DNS destination required");
        this.port=uri.getPort()==-1?443:uri.getPort();if(port<1||port>65535)throw new IllegalArgumentException("Invalid webhook port");
        this.key=Base64.getDecoder().decode(signingKeyBase64);if(key.length<32)throw new IllegalArgumentException("Webhook HMAC key must contain at least 32 random bytes");
        this.factory=Objects.requireNonNull(factory);try{this.addresses=Objects.requireNonNull(resolver).resolve(host).clone();if(addresses.length==0)throw new UnknownHostException(host);}catch(IOException error){throw new IllegalArgumentException("Webhook destination must resolve to approved addresses at startup",error);}
        try{this.destinationKey=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    @Override public String destinationKey(){return destinationKey;}
    @Override public Result send(Envelope envelope)throws Exception{
        Objects.requireNonNull(envelope.eventId());byte[] body=envelope.body().getBytes(StandardCharsets.UTF_8);if(body.length>4096)throw new IllegalArgumentException("Notification exceeds bounded envelope");
        // DNS is resolved and public-address validated at startup; the connected
        // address is pinned for this instance. Restart/reconfigure for DNS rotation.
        String timestamp=Long.toString(Instant.now().getEpochSecond());Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));String signature=HexFormat.of().formatHex(mac.doFinal((timestamp+"."+envelope.body()).getBytes(StandardCharsets.UTF_8)));
        try(Socket raw=new Socket()){
            ScheduledFuture<?> deadline=DEADLINES.schedule(()->{try{raw.close();}catch(IOException ignored){}},15,TimeUnit.SECONDS);
            try{
            raw.connect(new InetSocketAddress(addresses[0],port),3000);raw.setSoTimeout(8000);
            try(SSLSocket socket=(SSLSocket)factory.createSocket(raw,host,port,true)){
                var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");parameters.setServerNames(List.of(new SNIHostName(host)));parameters.setApplicationProtocols(new String[]{"http/1.1"});socket.setSSLParameters(parameters);socket.setSoTimeout(8000);socket.startHandshake();
                String rawPath=URI.create(uri.toASCIIString()).getRawPath();String path=rawPath==null||rawPath.isEmpty()?"/":rawPath;
                String headers="POST "+path+" HTTP/1.1\r\nHost: "+host+(port==443?"":":"+port)+"\r\nContent-Type: application/json\r\nContent-Length: "+body.length+"\r\nConnection: close\r\nIdempotency-Key: "+envelope.eventId()+"\r\nX-Quincai-Event-ID: "+envelope.eventId()+"\r\nX-Quincai-Timestamp: "+timestamp+"\r\nX-Quincai-Signature: v1="+signature+"\r\n\r\n";
                var out=socket.getOutputStream();out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();
                var input=socket.getInputStream();int[] budget={8192};
                for(int interim=0;interim<4;interim++){String status=line(input,budget);if(!status.matches("HTTP/1\\.[01] [1-5][0-9]{2}( .*)?"))throw new IOException("Invalid bounded webhook response");int code=Integer.parseInt(status.substring(9,12));while(!line(input,budget).isEmpty()){}if(code>=200)return new Result(code);}
                throw new IOException("Excessive interim webhook responses");
            }
            }finally{deadline.cancel(false);}
        }
    }
    private static String line(InputStream in,int[] budget)throws IOException{var bytes=new ByteArrayOutputStream();int previous=-1;while(true){if(--budget[0]<0)throw new IOException("Webhook headers exceed limit");int b=in.read();if(b<0)throw new EOFException("Incomplete webhook headers");if(previous=='\r'&&b=='\n')return bytes.toString(StandardCharsets.US_ASCII).substring(0,bytes.size()-1);if(b=='\n'||b==0||bytes.size()>2048)throw new IOException("Malformed webhook header");bytes.write(b);previous=b;}}
    static InetAddress[] publicAddresses(String host)throws IOException{var addresses=InetAddress.getAllByName(host);for(var a:addresses){byte[] b=a.getAddress();boolean blocked=a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress();if(b.length==4){int first=b[0]&255,second=b[1]&255;blocked|=first==0||first>=224||first==127||(first==100&&second>=64&&second<=127)||(first==169&&second==254)||(first==192&&second==0)||(first==198&&(second==18||second==19));}else if(b.length==16){blocked|=(b[0]&0xfe)==0xfc||((b[0]&255)==0x20&&(b[1]&255)==0x01&&(b[2]&255)==0x0d&&(b[3]&255)==0xb8);}if(blocked)throw new IOException("Webhook destination resolved to a prohibited address");}return addresses;}
}
