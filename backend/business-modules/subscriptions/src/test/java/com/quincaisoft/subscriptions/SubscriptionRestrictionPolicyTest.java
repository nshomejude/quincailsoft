package com.quincaisoft.subscriptions;

import com.quincaisoft.subscriptions.domain.SubscriptionRestrictionPolicy;
import com.quincaisoft.subscriptions.domain.SubscriptionRestrictionPolicy.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRestrictionPolicyTest {
    private static final UUID TENANT = UUID.randomUUID(), SUBSCRIPTION = UUID.randomUUID();
    private static final Instant NOTICE = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-18T00:00:00Z");
    private static Subscription subscription(Status status) { return new Subscription(TENANT,SUBSCRIPTION,2,status); }
    private static RestrictionEvidence evidence() {
        return new RestrictionEvidence(TENANT,SUBSCRIPTION,2,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),NOTICE);
    }
    private static Decision decide(Status status, RestrictionEvidence e, Instant time) {
        return SubscriptionRestrictionPolicy.evaluate(subscription(status),Operation.NEW_BUSINESS_POSTING,e,time);
    }
    @TestFactory List<DynamicTest> protectedOperationsSurviveEveryBillingStatus() {
        var tests = new ArrayList<DynamicTest>();
        for (var status:Status.values()) for (var op:Operation.values()) {
            if(op==Operation.NEW_BUSINESS_POSTING)continue;
            tests.add(DynamicTest.dynamicTest(status+" preserves "+op,()->{
                assertEquals(Decision.NOT_RESTRICTED_BY_BILLING,SubscriptionRestrictionPolicy.evaluate(subscription(status),op,evidence(),END));
                assertEquals(Decision.NOT_RESTRICTED_BY_BILLING,SubscriptionRestrictionPolicy.evaluate(subscription(status),op,null,END));
            }));
        }
        return tests;
    }
    @TestFactory List<DynamicTest> graceAndReviewRequiredForEveryRestrictableState() {
        var tests = new ArrayList<DynamicTest>();
        for(var status:List.of(Status.PAST_DUE,Status.SUSPENDED,Status.CANCELLED,Status.EXPIRED)) {
            tests.add(DynamicTest.dynamicTest(status+" without evidence",()->assertEquals(Decision.RESTRICTION_REQUIRES_REVIEW,decide(status,null,END))));
            tests.add(DynamicTest.dynamicTest(status+" before grace expiry",()->assertEquals(Decision.RESTRICTION_REQUIRES_REVIEW,decide(status,evidence(),END.minusNanos(1)))));
            tests.add(DynamicTest.dynamicTest(status+" at grace expiry",()->assertEquals(Decision.RESTRICT_NEW_POSTING,decide(status,evidence(),END))));
        }
        for(var status:List.of(Status.TRIAL,Status.ACTIVE,Status.GRACE))
            tests.add(DynamicTest.dynamicTest(status+" cannot be restricted by old notice",()->assertEquals(Decision.NOT_RESTRICTED_BY_BILLING,decide(status,evidence(),END.plusSeconds(100)))));
        return tests;
    }
    @Test void futureContractReviewCannotAuthorizeRestriction() {
        var e=new RestrictionEvidence(TENANT,SUBSCRIPTION,2,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),END.plusSeconds(1));
        assertEquals(Decision.RESTRICTION_REQUIRES_REVIEW,decide(Status.PAST_DUE,e,END));
        assertEquals(Decision.RESTRICT_NEW_POSTING,decide(Status.PAST_DUE,e,END.plusSeconds(1)));
    }
    @Test void otherTenantOtherSubscriptionAndStaleVersionCannotAuthorizeRestriction() {
        for(var e:List.of(
            new RestrictionEvidence(UUID.randomUUID(),SUBSCRIPTION,2,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),NOTICE),
            new RestrictionEvidence(TENANT,UUID.randomUUID(),2,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),NOTICE),
            new RestrictionEvidence(TENANT,SUBSCRIPTION,1,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),NOTICE)))
            assertEquals(Decision.RESTRICTION_REQUIRES_REVIEW,decide(Status.PAST_DUE,e,END));
    }
    @Test void noZeroOrNegativeGracePeriod() {
        for(var end:List.of(NOTICE,NOTICE.minusSeconds(1)))
            assertThrows(IllegalArgumentException.class,()->new RestrictionEvidence(TENANT,SUBSCRIPTION,2,UUID.randomUUID(),NOTICE,end,UUID.randomUUID(),NOTICE));
    }
    @Test void missingAuditReferencesAndInvalidVersionsAreRejected() {
        assertThrows(NullPointerException.class,()->new RestrictionEvidence(TENANT,SUBSCRIPTION,2,null,NOTICE,END,UUID.randomUUID(),NOTICE));
        assertThrows(NullPointerException.class,()->new RestrictionEvidence(TENANT,SUBSCRIPTION,2,UUID.randomUUID(),NOTICE,END,null,NOTICE));
        assertThrows(IllegalArgumentException.class,()->new Subscription(TENANT,SUBSCRIPTION,-1,Status.ACTIVE));
        assertThrows(IllegalArgumentException.class,()->new RestrictionEvidence(TENANT,SUBSCRIPTION,-1,UUID.randomUUID(),NOTICE,END,UUID.randomUUID(),NOTICE));
    }
    @Test void nullDecisionInputsAreRejected() {
        assertThrows(NullPointerException.class,()->SubscriptionRestrictionPolicy.evaluate(null,Operation.READ,null,END));
        assertThrows(NullPointerException.class,()->SubscriptionRestrictionPolicy.evaluate(subscription(Status.ACTIVE),null,null,END));
        assertThrows(NullPointerException.class,()->SubscriptionRestrictionPolicy.evaluate(subscription(Status.ACTIVE),Operation.READ,null,null));
    }
}
