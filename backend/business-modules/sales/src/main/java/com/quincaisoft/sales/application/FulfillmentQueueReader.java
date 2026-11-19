package com.quincaisoft.sales.application;

import com.quincaisoft.sales.api.FulfillmentService;
import com.quincaisoft.shared.ScopedTransactions;
import com.quincaisoft.shared.TenantContext;
import com.quincaisoft.shared.TenantScope;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.json.JsonMapper;

/** Scoped read projection and signed paging state for the live fulfillment queue. */
public final class FulfillmentQueueReader {
 private static final String SORT = "fulfillment-created-id-asc-v1";
 private final EntityManager em;
 private final ScopedTransactions tx;
 private final JsonMapper json;

 FulfillmentQueueReader(EntityManager em, ScopedTransactions tx, JsonMapper json) {
  this.em = em;
  this.tx = tx;
  this.json = json;
 }

 private record Cursor(TenantScope scope, long epoch, String status, String query, int limit,
                       Instant cutoff, Instant afterCreated, UUID afterId, Instant expiresAt, String sort) {}

 FulfillmentService.QueuePage page(String requestedStatus, String requestedQuery, int limit, String token) {
  String status = requestedStatus == null ? "PENDING" : requestedStatus.strip().toUpperCase(Locale.ROOT);
  String query = requestedQuery == null ? "" : requestedQuery.strip().toLowerCase(Locale.ROOT);
  if (!Set.of("ALL","PENDING","PARTIAL","DISPATCHED","COMPLETED").contains(status))
   throw new IllegalArgumentException("Unknown fulfillment queue status");
  if (query.length() > 100 || limit < 1 || limit > 100)
   throw new IllegalArgumentException("Search is limited to 100 characters and pages to 1–100 rows");
  return tx.execute(() -> {
   var scope = TenantContext.current();
   long epoch = TenantContext.authorizationEpoch();
   Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
   byte[] key = (byte[]) em.createNativeQuery("select secret from fulfillment_cursor_key where id=1")
       .getSingleResult();
   Cursor cursor = token == null || token.isBlank()
       ? new Cursor(scope,epoch,status,query,limit,now,null,null,now.plusSeconds(900),SORT)
       : decode(token,key,scope,epoch,status,query,limit,now);
   String sql = """
       select id,customer_name,warehouse_id,cast(extract(epoch from created_at)*1000000 as bigint),
              sale_id,customer_id,status,cast(business_date as text),proof_reference,cast(version as text),
              method,cast(scheduled_date as text),delivery_address,recipient_name,recipient_phone,notes
       from fulfillment_queue_v1
       where tenant_id=:tenant and company_id=:company and branch_id=:branch and created_at<=:cutoff
       """;
   if (!status.equals("ALL")) sql += " and status=:status";
   if (!query.isEmpty()) sql += """
        and (lower(customer_name) like :query escape '!' or lower(recipient_name) like :query escape '!'
             or cast(sale_id as text) like :query escape '!' or cast(id as text) like :query escape '!')
       """;
   if (cursor.afterId() != null)
    sql += " and (created_at>:afterCreated or (created_at=:afterCreated and id>:afterId))";
   sql += " order by created_at,id";
   var statement = em.createNativeQuery(sql,QueueRow.class)
       .setParameter("tenant",scope.tenantId()).setParameter("company",scope.companyId())
       .setParameter("branch",scope.branchId()).setParameter("cutoff",cursor.cutoff())
       .setMaxResults(limit + 1);
   if (!status.equals("ALL")) statement.setParameter("status",status);
   if (!query.isEmpty()) statement.setParameter("query","%" + query.replace("!","!!")
       .replace("%","!%").replace("_","!_") + "%");
   if (cursor.afterId() != null)
    statement.setParameter("afterCreated",cursor.afterCreated()).setParameter("afterId",cursor.afterId());
   List<?> results = statement.getResultList();
   var rows = results.stream().map(QueueRow.class::cast).toList();
   var items = new ArrayList<FulfillmentService.QueueEntry>();
   for (var row : rows.subList(0,Math.min(limit,rows.size()))) items.add(row.entry());
   String next = null;
   if (rows.size() > limit) {
    var last = rows.get(limit - 1);
    Instant created = Instant.EPOCH.plus(last.createdMicros(),ChronoUnit.MICROS);
    next = encode(new Cursor(scope,epoch,status,query,limit,cursor.cutoff(),created,last.id(),
        cursor.expiresAt(),SORT),key);
   }
   return new FulfillmentService.QueuePage(List.copyOf(items),next);
  });
 }

 public record QueueRow(UUID id,String customerName,UUID warehouseId,Long createdMicros,
                        UUID saleId,UUID customerId,String status,String businessDate,String proofReference,
                        String version,String method,String scheduledDate,String address,String recipientName,
                        String recipientPhone,String notes) {
  FulfillmentService.QueueEntry entry() {
   FulfillmentService.DeliveryDetails details = null;
   if (method != null || scheduledDate != null || address != null || recipientName != null ||
       recipientPhone != null || notes != null)
    details = new FulfillmentService.DeliveryDetails(method,
        scheduledDate == null ? null : LocalDate.parse(scheduledDate),address,recipientName,recipientPhone,notes);
   var task = new FulfillmentService.Fulfillment(id,saleId,customerId,status,
       LocalDate.parse(businessDate),proofReference,version,details);
   return new FulfillmentService.QueueEntry(task,customerName,warehouseId);
  }
 }

 private Cursor decode(String token,byte[] key,TenantScope scope,long epoch,String status,String query,
                       int limit,Instant now) {
  try {
   if (token.length() > 4096) throw new IllegalArgumentException();
   String[] parts = token.split("\\.",-1);
   if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0],key),Base64.getUrlDecoder().decode(parts[1])))
    throw new IllegalArgumentException();
   var cursor = json.readValue(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),Cursor.class);
   if (!scope.equals(cursor.scope()) || epoch != cursor.epoch() || !status.equals(cursor.status()) ||
       !query.equals(cursor.query()) || limit != cursor.limit() || !SORT.equals(cursor.sort()) ||
       cursor.cutoff() == null || cursor.cutoff().isAfter(now) || cursor.expiresAt() == null ||
       !cursor.expiresAt().isAfter(now) || cursor.afterCreated() == null || cursor.afterId() == null ||
       cursor.afterCreated().isAfter(cursor.cutoff())) throw new IllegalArgumentException();
   return cursor;
  } catch (RuntimeException invalid) {
   throw new IllegalArgumentException("Cursor is invalid, expired or belongs to another scope or filter",invalid);
  }
 }

 private String encode(Cursor cursor,byte[] key) {
  String payload = Base64.getUrlEncoder().withoutPadding()
      .encodeToString(json.writeValueAsString(cursor).getBytes(StandardCharsets.UTF_8));
  return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload,key));
 }

 private static byte[] sign(String payload,byte[] key) {
  try {
   var mac = Mac.getInstance("HmacSHA256");
   mac.init(new SecretKeySpec(key,"HmacSHA256"));
   return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
  } catch (java.security.GeneralSecurityException unavailable) {
   throw new IllegalStateException("Cursor signing unavailable",unavailable);
  }
 }
}
