package com.quincaisoft.subscriptions.application;
import com.quincaisoft.subscriptions.infrastructure.SubscriptionInvoice;
import com.quincaisoft.subscriptions.infrastructure.ManualSubscriptionPayment;
import java.util.Set;

/** Escaped immutable billing snapshots, with no external assets or executable markup. */
public final class SubscriptionHtml {
    private SubscriptionHtml(){}
    private static boolean french(String locale){if(!Set.of("en","fr").contains(locale))throw new IllegalArgumentException("EN/FR locale required");return locale.equals("fr");}
    public static String invoice(SubscriptionInvoice i,String locale){boolean fr=french(locale);String kind=i.kind.equals("CREDIT_NOTE")?(fr?"Avoir d’abonnement":"Subscription credit note"):(fr?"Facture d’abonnement":"Subscription invoice");
        return start(locale,kind)+"<h1>"+esc(i.issuerName)+"</h1><p>"+esc(i.issuerTaxReference)+"</p><h2>"+kind+"</h2><p>"+esc(i.number)+"</p><dl>"+
            field(fr?"Client":"Customer",i.customerName)+field(fr?"Référence fiscale":"Tax reference",i.customerTaxReference)+field(fr?"Juridiction":"Jurisdiction",i.jurisdiction)+field(fr?"Description":"Description",i.description)+field(fr?"Période":"Period",i.periodStart+" — "+i.periodEnd)+field(fr?"Échéance":"Due date",i.dueDate.toString())+
            (i.originalId==null?"":field(fr?"Facture originale (ID)":"Original invoice ID",i.originalId.toString()))+"</dl><table><tbody>"+
            row(fr?"Sous-total":"Subtotal",i.subtotalXaf)+row(fr?"Remise":"Discount",i.discountXaf)+row(fr?"Taxe":"Tax",i.taxXaf)+row("Total",i.totalXaf)+"</tbody></table><p>"+esc(i.createdAt.toString())+"</p><p class=note>"+(fr?"Document de facturation de la plateforme. Ne constitue pas une preuve de paiement ni de remboursement. Référence technique non certifiée fiscalement.":"Platform billing document. Not evidence of payment or refund settlement. Technical reference is not fiscally certified.")+"</p></body></html>";
    }
    public static String receipt(ManualSubscriptionPayment p,SubscriptionInvoice i,String locale){boolean fr=french(locale);if(!"MANUALLY_RECONCILED".equals(p.status))throw new IllegalStateException("Only independently reconciled payments have receipts");
        return start(locale,fr?"Reçu d’abonnement":"Subscription receipt")+"<h1>"+esc(i.issuerName)+"</h1><h2>"+(fr?"Reçu — rapprochement bancaire manuel":"Receipt — manual bank reconciliation")+"</h2><dl>"+
            field(fr?"Référence du reçu":"Receipt reference","SUB-RCPT-"+p.id)+field(fr?"Facture":"Invoice",i.number)+field(fr?"Client":"Customer",i.customerName)+field(fr?"Montant reçu (XAF)":"Received amount (XAF)",Long.toString(p.amountXaf))+field(fr?"Banque / référence":"Bank / reference",p.bankReference)+field(fr?"Empreinte SHA-256 de la preuve":"Evidence SHA-256",p.evidenceSha256)+field(fr?"Enregistré par":"Recorded by",p.recordedBy.toString())+field(fr?"Vérifié par":"Reviewed by",p.reviewedBy.toString())+field(fr?"Date de vérification":"Review date",p.reviewedAt.toString())+field(fr?"Motif":"Reason",p.reason)+"</dl><p class=note>"+(fr?"Statut : MANUALLY_RECONCILED. Attestation de deux opérateurs distincts, sans vérification automatique du prestataire. Paiement d’abonnement séparé de la caisse du commerçant.":"Status: MANUALLY_RECONCILED. Attested by two separate operators; not automatically verified by a payment provider. Subscription payment is separate from merchant cash.")+"</p></body></html>";
    }
    private static String start(String locale,String title){return "<!doctype html><html lang=\""+locale+"\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"+esc(title)+"</title><style>body{font:14px system-ui;color:#0B2545;max-width:760px;margin:32px auto;padding:24px}h2{border-bottom:3px solid #38BDF8;padding-bottom:12px}dt{font-weight:600;margin-top:12px}dd{margin:4px 0;overflow-wrap:anywhere}table{width:100%;border-collapse:collapse}td,th{padding:10px;border-bottom:1px solid #ddd;text-align:left}.note{font-size:12px;color:#475569}@media print{body{margin:0;padding:12mm}}</style></head><body>";}
    private static String field(String key,String value){return "<dt>"+esc(key)+"</dt><dd>"+esc(value)+"</dd>";}
    private static String row(String key,long value){return "<tr><th>"+esc(key)+"</th><td>"+value+" XAF</td></tr>";}
    public static String esc(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
