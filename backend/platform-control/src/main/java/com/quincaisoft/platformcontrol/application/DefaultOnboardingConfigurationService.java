package com.quincaisoft.platformcontrol.application;

import com.quincaisoft.platformcontrol.api.*;
import com.quincaisoft.platformcontrol.infrastructure.*;
import jakarta.persistence.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Service
public class DefaultOnboardingConfigurationService implements OnboardingConfigurationService {
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final OnboardingIdentityVerifier verifier;
    private final JsonMapper json = JsonMapper.builder().build();
    public DefaultOnboardingConfigurationService(EntityManager em, PlatformTransactionManager manager, OnboardingIdentityVerifier verifier) {
        this.em = em; this.tx = new TransactionTemplate(manager); this.verifier = verifier;
    }
    private UUID actor(String capability) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SCOPE_" + capability)))
            throw new AccessDeniedException("Platform capability required: " + capability);
        if (jwt.getToken().getSubject() == null) throw new AccessDeniedException("Platform identity required");
        try { return UUID.fromString(jwt.getToken().getSubject()); }
        catch (IllegalArgumentException ex) { throw new AccessDeniedException("Platform identity required"); }
    }
    private <T> T scoped(String capability, Supplier<T> action) {
        UUID actor = actor(capability);
        return tx.execute(status -> {
            em.createNativeQuery("select set_config('app.platform_actor',:a,true)").setParameter("a",actor.toString()).getSingleResult();
            var rows = em.createNativeQuery("select actor_id from platform_operator where actor_id=:a and active").setParameter("a",actor).getResultList();
            if (rows.size() != 1) throw new AccessDeniedException("Enrolled active platform operator required");
            return action.get();
        });
    }
    @Override public Configuration get(UUID id) {
        Objects.requireNonNull(id,"requestId");
        return scoped("platform:tenants:read", () -> view(require(id,LockModeType.NONE)));
    }
    @Override public Configuration configure(UUID id, Configure input) {
        if (id == null || input == null || input.operationId() == null || input.requestVersion() < 0) throw new IllegalArgumentException("Request, operation and version required");
        validate(input.profile());
        return command(id,input.operationId(),"platform:tenants:write","CONFIGURE",input,() -> {
            var request = em.find(OnboardingRequest.class,id,LockModeType.PESSIMISTIC_WRITE);
            if (request == null) throw new NoSuchElementException("Onboarding request not found");
            if (!"APPROVED_FOR_PROVISIONING".equals(request.status) || request.version != input.requestVersion()) throw new IllegalStateException("Approved request version required");
            if (em.find(OnboardingConfiguration.class,id) != null) throw new IllegalStateException("Configuration already exists; submitted identity cannot be silently replaced");
            var p = input.profile(); var c = new OnboardingConfiguration(); c.requestId=id;
            c.legalName=p.legalName(); c.registrationNumber=p.registrationNumber(); c.address=p.address();
            c.ownerSubject=p.ownerSubject(); c.ownerEmail=p.ownerEmail(); c.region=p.region(); c.timezone=p.timezone();
            c.locale=p.locale(); c.documentPrefix=p.documentPrefix(); c.openingMode=p.openingMode();
            c.privacyVersion=p.privacyVersion(); c.termsVersion=p.termsVersion(); c.planCode=p.planCode();
            c.configuredBy=actor("platform:tenants:write"); c.status="AWAITING_IDENTITY"; c.createdAt=now();
            em.persist(c); em.flush(); event(c,input.operationId(),"OnboardingConfigured.v1",c.configuredBy); return view(c);
        });
    }
    @Override public Configuration verifyIdentity(UUID id, VerifyIdentity input) {
        if (id == null || input == null || input.operationId() == null || input.version() < 0 || input.ownerToken() == null
                || input.ownerToken().isBlank() || input.ownerToken().length() > 32768) throw new IllegalArgumentException("Operation, version and owner identity proof required");
        // Never serialize the raw token into request journals, logs or outbox payloads.
        String material = input.version() + ":" + sha(input.ownerToken());
        return command(id,input.operationId(),"platform:tenants:verify","VERIFY_IDENTITY",material,() -> {
            var c=require(id,LockModeType.PESSIMISTIC_WRITE);
            if(c.version!=input.version() || !"AWAITING_IDENTITY".equals(c.status)) throw new IllegalStateException("Identity verification version changed");
            var proof=verifier.verify(input.ownerToken());
            if (proof == null || !c.ownerSubject.equals(proof.subject()) || !c.ownerEmail.equals(proof.email()) || proof.issuer()==null || proof.issuer().isBlank())
                throw new AccessDeniedException("Verified identity does not match configured owner");
            c.identityVerifiedAt=now(); c.identityIssuer=proof.issuer(); c.status="IDENTITY_VERIFIED";
            em.flush(); event(c,input.operationId(),"OnboardingIdentityVerified.v1",actor("platform:tenants:verify")); return view(c);
        });
    }
    @Override public Configuration approve(UUID id, Approval input) {
        if (id == null || input == null || input.operationId() == null || input.version()<0) throw new IllegalArgumentException("Operation and version required");
        return command(id,input.operationId(),"platform:tenants:approve","APPROVE_CONFIGURATION",input,() -> {
            var c=require(id,LockModeType.PESSIMISTIC_WRITE); UUID actor=actor("platform:tenants:approve");
            if(c.version!=input.version() || !"IDENTITY_VERIFIED".equals(c.status)) throw new IllegalStateException("Identity-verified current configuration required");
            var request=em.find(OnboardingRequest.class,id);
            if(actor.equals(c.configuredBy) || actor.equals(request.requestedBy)) throw new AccessDeniedException("Independent configuration reviewer required");
            c.approvedBy=actor; c.status="APPROVED_CONFIGURATION";
            em.flush(); event(c,input.operationId(),"OnboardingConfigurationApproved.v1",actor); return view(c);
        });
    }
    private Configuration command(UUID id, UUID operation, String scope, String kind, Object input, Supplier<Configuration> action) {
        return scoped(scope,() -> {
            UUID actor=actor(scope);
            String hash=sha(json.writeValueAsString(List.of(id,actor,kind,input)));
            em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:k,0))").setParameter("k","onboarding-config:"+operation).getSingleResult();
            var old=em.find(OnboardingCommand.class,operation);
            if(old!=null) {
                if(!old.requestId.equals(id) || !old.actorId.equals(actor) || !old.requestHash.equals(hash)) throw new IllegalStateException("Operation reused with a different request or actor");
                return json.readValue(old.responseText,Configuration.class);
            }
            Configuration result=action.get();
            var cmd=new OnboardingCommand(); cmd.operationId=operation;cmd.requestId=id;cmd.actorId=actor;cmd.requestHash=hash;
            cmd.responseText=json.writeValueAsString(result);cmd.createdAt=now();em.persist(cmd);em.flush();return result;
        });
    }
    private void event(OnboardingConfiguration c,UUID operation,String type,UUID actor) {
        var event=new OnboardingEvent();event.id=UUID.randomUUID();event.requestId=c.requestId;event.operationId=operation;
        event.actorId=actor;event.eventType=type;event.configurationVersion=c.version;event.createdAt=now();em.persist(event);
    }
    private OnboardingConfiguration require(UUID id,LockModeType lock) {
        var c=em.find(OnboardingConfiguration.class,id,lock);if(c==null)throw new NoSuchElementException("Onboarding configuration not found");return c;
    }
    private Configuration view(OnboardingConfiguration c) {
        return new Configuration(c.requestId,new Profile(c.legalName,c.registrationNumber,c.address,c.ownerSubject,c.ownerEmail,c.region,c.timezone,c.locale,c.documentPrefix,c.openingMode,c.privacyVersion,c.termsVersion,c.planCode),c.configuredBy,c.approvedBy,c.status,Long.toString(c.version),c.identityVerifiedAt,c.identityIssuer,c.createdAt);
    }
    private static void validate(Profile p) {
        if(p==null || p.ownerSubject()==null)throw new IllegalArgumentException("Company profile and owner required");
        text(p.legalName(),200);text(p.registrationNumber(),100);text(p.address(),500);text(p.ownerEmail(),254);
        text(p.region(),80);text(p.timezone(),80);text(p.privacyVersion(),100);text(p.termsVersion(),100);text(p.planCode(),80);
        if(!p.ownerEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || (p.locale()==null || !Set.of("en","fr").contains(p.locale()))
                || p.documentPrefix()==null || !p.documentPrefix().matches("[A-Z0-9-]{1,16}") || (p.openingMode()==null || !Set.of("EMPTY","CSV","SUPERVISED").contains(p.openingMode())))
            throw new IllegalArgumentException("Valid contact, locale, document prefix and opening mode required");
        try { ZoneId.of(p.timezone()); } catch(DateTimeException ex) { throw new IllegalArgumentException("Valid IANA timezone required"); }
    }
    private static void text(String value,int max) {
        if(value==null || value.isBlank() || value.length()>max || !value.equals(value.trim()) || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Nonblank bounded configuration text required");
    }
    private static Instant now(){return Instant.now().truncatedTo(ChronoUnit.MICROS);}
    private static String sha(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
