package com.quincaisoft.platformcontrol.security;
import org.junit.jupiter.api.Test;import org.springframework.security.oauth2.jwt.Jwt;import java.util.UUID;import static org.junit.jupiter.api.Assertions.*;
class PlatformSubjectValidatorTest{
 @Test void missingMalformedAndNoncanonicalSubjectsCannotReachOperatorServices(){var validator=new PlatformSubjectValidator();assertTrue(validator.validate(Jwt.withTokenValue("synthetic").header("alg","RS256").claim("aud","platform").build()).hasErrors());for(String sub:new String[]{"operator","1-1-1-1-1",""})assertTrue(validator.validate(Jwt.withTokenValue("synthetic").header("alg","RS256").subject(sub).build()).hasErrors());assertFalse(validator.validate(Jwt.withTokenValue("synthetic").header("alg","RS256").subject(UUID.randomUUID().toString()).build()).hasErrors());}
}
