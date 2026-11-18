package com.quincaisoft.subscriptions.notifications;
import com.quincaisoft.subscriptions.api.BillingNotificationDispatcher;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.stream.Collectors;
@Configuration @EnableScheduling @ConditionalOnProperty(name="quincai.billing.notifications.enabled",havingValue="true")
public class BillingNotificationConfiguration {
 @Bean BillingNotificationTransport billingNotificationTransport(@Value("${quincai.billing.notifications.endpoint}")String endpoint,@Value("${quincai.billing.notifications.allowed-hosts}")String hosts,@Value("${quincai.billing.notifications.signing-key}")String secret){return new PinnedHttpsBillingTransport(endpoint,Arrays.stream(hosts.split(",")).map(String::trim).map(s->s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet()),secret);}
 @Bean BillingNotificationDispatcher billingNotificationDispatcher(EntityManager em,PlatformTransactionManager manager,JsonMapper json,@Value("${quincai.billing.notifications.worker-actor}")UUID actor,BillingNotificationTransport transport){return new DefaultBillingNotificationDispatcher(em,manager,json,actor,transport);}
 @Bean NotificationTick billingNotificationTick(BillingNotificationDispatcher dispatcher){return new NotificationTick(dispatcher);}
 public static class NotificationTick {private final BillingNotificationDispatcher dispatcher;NotificationTick(BillingNotificationDispatcher dispatcher){this.dispatcher=dispatcher;}
  @Scheduled(fixedDelayString="${quincai.billing.notifications.delay-ms:5000}") public void tick(){for(int n=0;n<10;n++)if(!dispatcher.dispatchOne())break;}
 }
}
