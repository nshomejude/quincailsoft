package com.quincaisoft.subscriptions;
import com.quincaisoft.subscriptions.application.SubscriptionHtml;
import com.quincaisoft.subscriptions.infrastructure.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SubscriptionHtmlTest {
 @Test void untrustedBillingTextIsEscapedAndPendingPaymentIsNotAReceipt(){
  var i=new SubscriptionInvoice();i.kind="INVOICE";i.number="SUB-TEST";i.issuerName="<script>alert(1)</script>";i.customerName="<img src=x onerror=alert(1)>";i.issuerTaxReference="TEST";i.customerTaxReference="";i.description="A & B";i.jurisdiction="TEST";i.periodStart=LocalDate.of(2026,11,1);i.periodEnd=LocalDate.of(2026,11,30);i.dueDate=i.periodEnd;i.createdAt=Instant.parse("2026-11-18T00:00:00Z");i.subtotalXaf=100;i.totalXaf=100;
  for(var locale:new String[]{"en","fr"}){var html=SubscriptionHtml.invoice(i,locale);assertFalse(html.contains("<script>"));assertFalse(html.contains("<img"));assertTrue(html.contains("&lt;script&gt;"));assertTrue(html.contains("A &amp; B"));assertTrue(html.contains("100 XAF"));}
  assertThrows(IllegalArgumentException.class,()->SubscriptionHtml.invoice(i,"<script>"));
  var p=new ManualSubscriptionPayment();p.status="RECORDED";assertThrows(IllegalStateException.class,()->SubscriptionHtml.receipt(p,i,"en"));
  p.status="MANUALLY_RECONCILED";p.id=UUID.randomUUID();p.recordedBy=UUID.randomUUID();p.reviewedBy=UUID.randomUUID();p.reviewedAt=i.createdAt;p.bankReference="<script>bank</script>";p.evidenceSha256="a".repeat(64);p.reason="Test";p.amountXaf=100;
  var html=SubscriptionHtml.receipt(p,i,"en");assertFalse(html.contains("<script>"));assertTrue(html.contains("MANUALLY_RECONCILED"));assertTrue(html.contains("not automatically verified"));
 }
}
