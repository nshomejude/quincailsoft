package com.quincaisoft.security;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import java.util.ArrayList;
@Configuration @EnableMethodSecurity
public class SecurityConfiguration {
 @Bean @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(tools.jackson.databind.json.JsonMapper.class) tools.jackson.databind.json.JsonMapper jsonMapper(){return tools.jackson.databind.json.JsonMapper.builder().build();}
 @Bean JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,@Value("${quincai.security.audience}") String audience){
  NimbusJwtDecoder decoder=JwtDecoders.fromIssuerLocation(issuer);
  OAuth2TokenValidator<Jwt> aud=jwt -> jwt.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","API audience required",null));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),aud));return decoder;
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  var scopes=new JwtGrantedAuthoritiesConverter();
  var converter=new JwtAuthenticationConverter();
  converter.setJwtGrantedAuthoritiesConverter(jwt->{var authorities=new ArrayList<>(scopes.convert(jwt));var permissions=jwt.getClaimAsStringList("permissions");if(permissions!=null)permissions.forEach(p->authorities.add(new SimpleGrantedAuthority(p)));return authorities;});
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.accessDeniedHandler((request,response,error)->ApiErrors.write(request,response,403,"ACCESS_DENIED","Access denied")))
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter)).authenticationEntryPoint((request,response,error)->ApiErrors.write(request,response,401,"UNAUTHENTICATED","Valid bearer token required")))
   .addFilterBefore(new RequestIdentityFilter(),BearerTokenAuthenticationFilter.class)
   .addFilterAfter(new TenantScopeFilter(),BearerTokenAuthenticationFilter.class).build();
 }
}
