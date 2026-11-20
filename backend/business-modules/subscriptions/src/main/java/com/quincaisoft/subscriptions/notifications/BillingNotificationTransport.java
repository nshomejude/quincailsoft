package com.quincaisoft.subscriptions.notifications;
import java.util.UUID;
/** Notification HTTP acceptance only; never proof of payment or legal receipt. */
public interface BillingNotificationTransport {
    record Envelope(UUID eventId,String body){}
    record Result(int statusCode){}
    String destinationKey();
    Result send(Envelope envelope) throws Exception;
}
