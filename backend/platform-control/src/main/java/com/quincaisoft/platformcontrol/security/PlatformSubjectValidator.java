package com.quincaisoft.platformcontrol.security;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
/** An enrolled platform operator is a concrete UUID identity, not merely a valid issuer token. */
public final class PlatformSubjectValidator implements OAuth2TokenValidator<Jwt> {
 public OAuth2TokenValidatorResult validate(Jwt jwt){try{String subject=jwt.getSubject();if(subject==null||!UUID.fromString(subject).toString().equalsIgnoreCase(subject))throw new IllegalArgumentException();return OAuth2TokenValidatorResult.success();}catch(IllegalArgumentException e){return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Platform subject must be a UUID",null));}}
}
