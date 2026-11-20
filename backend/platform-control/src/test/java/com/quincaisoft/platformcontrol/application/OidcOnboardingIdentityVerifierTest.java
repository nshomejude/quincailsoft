package com.quincaisoft.platformcontrol.application;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OidcOnboardingIdentityVerifierTest {
    static KeyPair key;
    static final String ISSUER="https://identity.example.test", AUDIENCE="quincai-api";
    static final UUID SUBJECT=UUID.randomUUID();
    @BeforeAll static void keys() throws Exception {var g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);key=g.generateKeyPair();}
    OidcOnboardingIdentityVerifier verifier() {
        var decoder=NimbusJwtDecoder.withPublicKey((RSAPublicKey)key.getPublic()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return new OidcOnboardingIdentityVerifier(ISSUER,AUDIENCE,decoder);
    }
    JWTClaimsSet.Builder claims() {return new JWTClaimsSet.Builder().issuer(ISSUER).subject(SUBJECT.toString()).audience(AUDIENCE)
        .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("email","owner@example.test").claim("email_verified",true);}
    String sign(JWTClaimsSet claims,PrivateKey signer) throws Exception {var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims);jwt.sign(new RSASSASigner((RSAPrivateKey)signer));return jwt.serialize();}
    String sign(JWTClaimsSet.Builder claims) throws Exception {return sign(claims.build(),key.getPrivate());}
    @Test void verifiesSignedBoundIdentity() throws Exception {
        var owner=verifier().verify(sign(claims()));assertEquals(SUBJECT,owner.subject());assertEquals(ISSUER,owner.issuer());assertEquals("owner@example.test",owner.email());
    }
    @Test void rejectsWrongIssuer() throws Exception {var token=sign(claims().issuer("https://attacker.example.test"));assertThrows(AccessDeniedException.class,()->verifier().verify(token));}
    @Test void rejectsWrongAudience() throws Exception {var token=sign(claims().audience("platform-admin"));assertThrows(AccessDeniedException.class,()->verifier().verify(token));}
    @Test void rejectsUnverifiedContact() throws Exception {var token=sign(claims().claim("email_verified",false));assertThrows(AccessDeniedException.class,()->verifier().verify(token));}
    @Test void rejectsExpiredAndMissingExpiry() throws Exception {
        var expired=sign(claims().expirationTime(Date.from(Instant.now().minusSeconds(600))));
        var missing=sign(claims().expirationTime(null));
        assertThrows(AccessDeniedException.class,()->verifier().verify(expired));assertThrows(AccessDeniedException.class,()->verifier().verify(missing));
    }
    @Test void rejectsInvalidSignature() throws Exception {
        var g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);var other=g.generateKeyPair();
        var token=sign(claims().build(),other.getPrivate());assertThrows(AccessDeniedException.class,()->verifier().verify(token));
    }
    @Test void rejectsMissingEmail() throws Exception {var token=sign(claims().claim("email",null));assertThrows(AccessDeniedException.class,()->verifier().verify(token));}
}
