package com.quincaisoft.platformcontrol.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="platform_onboarding_configuration")
public class OnboardingConfiguration {
    @Id @Column(name="request_id") public UUID requestId;
    @Column(name="legal_name") public String legalName;
    @Column(name="registration_number") public String registrationNumber;
    public String address;
    @Column(name="owner_subject") public UUID ownerSubject;
    @Column(name="owner_email") public String ownerEmail;
    public String region;
    public String timezone;
    public String locale;
    @Column(name="document_prefix") public String documentPrefix;
    @Column(name="opening_mode") public String openingMode;
    @Column(name="privacy_version") public String privacyVersion;
    @Column(name="terms_version") public String termsVersion;
    @Column(name="plan_code") public String planCode;
    @Column(name="configured_by") public UUID configuredBy;
    @Column(name="approved_by") public UUID approvedBy;
    public String status;
    @Version public long version;
    @Column(name="identity_verified_at") public Instant identityVerifiedAt;
    @Column(name="identity_issuer") public String identityIssuer;
    @Column(name="created_at") public Instant createdAt;
}
