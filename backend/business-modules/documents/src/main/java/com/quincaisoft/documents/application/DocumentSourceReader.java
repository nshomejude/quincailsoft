package com.quincaisoft.documents.application;

import com.quincaisoft.documents.api.*;
import com.quincaisoft.documents.api.DocumentWorkspace.*;
import com.quincaisoft.shared.*;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;import java.util.*;

/** Fixed projection adapters. No caller can supply a table, column, label dictionary or source JSON. */
@Component
public class DocumentSourceReader {
 private final EntityManager em;private final DocumentService originals;private final JsonMapper json;
 public DocumentSourceReader(EntityManager em,DocumentService originals,JsonMapper json){this.em=em;this.originals=originals;this.json=json;}
 private static Template t(String id,String en,String fr,String paper,String permission,String basis){return new Template(id,en,fr,paper,permission,basis);}
 private static final List<Template> TEMPLATES=List.of(
  t("DOC01","Sales receipt","Ticket de vente","80mm","sales:read","Immutable original invoice; this receipt does not assert cash collection"),
  t("DOC02","Commercial invoice copy","Copie de facture commerciale","A4","sales:read","Immutable issued invoice; operational, not DGI certified"),
  t("DOC03","Quotation copy","Copie de devis","A4","quotations:read","Immutable issued quotation"),
  t("DOC04","Proforma quotation","Devis proforma","A4","quotations:read","Issued quotation content; not a fiscal invoice or proof of payment"),
  t("DOC05","Purchase order capture","Capture du bon de commande","A4","purchasing:read","Approved purchase terms and receipt progress at capture time"),
  t("DOC06","Goods received note","Bon de réception","A4","purchasing:read","Posted receipt quantities; product labels fixed at capture time"),
  t("DOC07","Supplier return note","Bon de retour fournisseur","A4","purchasing:read","Posted supplier-return quantities; product labels fixed at capture time"),
  t("DOC08","Customer balances statement","Relevé des soldes client","A4","finance:read","Current sale receivables and deposit liabilities shown separately; no inferred aging or netting"),
  t("DOC09","Customer receipt/deposit voucher","Reçu client ou dépôt","80mm","tenders:read","Immutable cash receipt or deposit fact; allocations are not new collections"),
  t("DOC10","Credit note copy","Copie de note de crédit","A4","returns:read","Immutable original credit note"),
  t("DOC11","Refund voucher","Bon de remboursement","A4","tenders:read","Immutable posted refund fact"),
  t("DOC13","Stock count sheet","Feuille de comptage","A4","inventory:read","Captured count source and counted quantities; no silently summed mixed units"),
  t("DOC14","Expense voucher","Pièce de dépense","A4","expenses:read","Posted expense and its linked payment fact"),
  t("DOC15","Cash session closing sheet","Clôture de caisse","A4","cash:read","Closed cash session; expected cash reconciles to immutable cash movements"),
  t("DOC16","Transfer progress note","Bon de transfert","A4","inventory:read","Dispatched transfer quantities and received progress at capture time"),
  t("DOC21","Operational journal voucher","Pièce de journal opérationnel","A4","finance:read","Immutable operational journal debit/credit lines; not certified statutory accounts"),
  t("DOC24","Production job label","Étiquette de production","80mm","inventory:read","Posted cut/repack source and output quantities; does not invent length/yield measurements"));
 public List<Template> templates(){return TEMPLATES;}
 public Template template(String id){return TEMPLATES.stream().filter(t->t.id().equals(id)).findFirst().orElseThrow(()->new NoSuchElementException("Document family not implemented"));}
 public void authorize(String id){String permission=template(id).permission();var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(permission)||a.getAuthority().equals("SCOPE_"+permission)))throw new AccessDeniedException("Source document permission required: "+permission);}
 private String originalKind(String id){return switch(id){case "DOC01","DOC02"->"INVOICE";case "DOC03","DOC04"->"QUOTATION";case "DOC10"->"CREDIT_NOTE";default->null;};}
 @SuppressWarnings("unchecked") private List<Object[]> query(String columns,String view,String tail,UUID id,String order,int max){
  var s=TenantContext.current();var q=em.createNativeQuery("select "+columns+" from "+view+" where tenant_id=:t and company_id=:c and branch_id=:b "+tail+(order.isBlank()?"":" order by "+order));q.setParameter("t",s.tenantId()).setParameter("c",s.companyId()).setParameter("b",s.branchId());if(id!=null)q.setParameter("id",id);return q.setMaxResults(max).getResultList();
 }
 public List<SourceCandidate> sources(String id){authorize(id);String kind=originalKind(id);List<Object[]> rows;
  if(kind!=null)rows=query("source_id,number,kind,business_date","document_snapshot","and kind='"+kind+"'",null,"issued_at desc,id",200);
  else rows=switch(id){
   case "DOC05"->query("distinct id,cast(id as text),status,business_date","document_purchase_v1","and status in('APPROVED','PARTIALLY_RECEIVED','RECEIVED')",null,"business_date desc,id",200);
   case "DOC06"->query("distinct id,cast(id as text),'POSTED',business_date","document_receipt_v1","",null,"business_date desc,id",200);
   case "DOC07"->query("distinct id,cast(id as text),'POSTED',business_date","document_supplier_return_v1","",null,"business_date desc,id",200);
   case "DOC08"->query("distinct customer_id,party,'CURRENT',null::date","document_customer_statement_v1","",null,"party,customer_id",200);
   case "DOC09","DOC11"->query("id,cast(id as text),kind,business_date","document_tender_v1",id.equals("DOC09")?"and kind in('RECEIPT','DEPOSIT')":"and kind='REFUND'",null,"business_date desc,id",200);
   case "DOC13","DOC24"->query("id,description,status,business_date","document_inventory_job_v1",id.equals("DOC13")?"and kind='COUNT' and status='POSTED'":"and kind in('CUT','REPACK') and status='POSTED'",null,"business_date desc,id",200);
   case "DOC14"->query("id,payee,status,business_date","document_expense_v1","and status='POSTED'",null,"business_date desc,id",200);
   case "DOC15"->query("id,till_code,status,business_date","report_cash_v1","and status='CLOSED'",null,"business_date desc,id",200);
   case "DOC16"->query("id,description,status,business_date","document_transfer_v1","and status in('IN_TRANSIT','PARTIALLY_RECEIVED','RECEIVED')",null,"business_date desc,id",200);
   case "DOC21"->query("distinct id,source_type,'POSTED',business_date","document_journal_v1","",null,"business_date desc,id",200);
   default->throw new NoSuchElementException();};
  return rows.stream().map(r->new SourceCandidate((UUID)r[0],text(r[1]),text(r[2]),date(r[3]))).toList();
 }
 public record Material(Body body,DocumentService.Settings originalIssuer){}
 public Material read(String id,UUID source){authorize(id);var template=template(id);String kind=originalKind(id);
  if(kind!=null){var d=originals.list(source).stream().filter(x->x.kind().equals(kind)).findFirst().orElseThrow(()->new NoSuchElementException("Issued source document not found"));var parsed=json.readTree(d.content());var src=json.treeToValue(parsed.get("source"),DocumentService.Issue.class);var issuer=json.treeToValue(parsed.get("issuer"),DocumentService.Settings.class);List<List<String>> rows=src.items().stream().map(i->List.of(i.sku()==null?"":i.sku(),i.description(),i.baseUnit(),i.quantityMilli(),i.unitPriceXaf(),i.totalXaf())).toList();return new Material(new Body(d.number(),"ISSUED",parsed.get("templateVersion").asText(),d.businessDate(),src.customerId()==null?"":src.customerId().toString(),List.of("SKU|Référence","Description|Désignation","Unit|Unité","Quantity milli|Quantité milli","Unit price XAF|Prix unitaire XAF","Amount XAF|Montant XAF"),rows,d.totalXaf(),template.basis()),issuer);}
  List<Object[]> r;List<String> columns;String status="POSTED",version="",party="",total="";LocalDate day=null;List<List<String>> rows=new ArrayList<>();
  switch(id){
   case "DOC05"->{r=required(query("business_date,status,version,party,total_xaf,description,base_unit,quantity_milli,received_milli,unit_price_xaf,line_total_xaf","document_purchase_v1","and id=:id and status in('APPROVED','PARTIALLY_RECEIVED','RECEIVED')",source,"line_id",5001));var h=r.getFirst();day=date(h[0]);status=text(h[1]);version=text(h[2]);party=text(h[3]);total=text(h[4]);columns=List.of("Product|Produit","Unit|Unité","Ordered milli|Commandé milli","Received milli|Reçu milli","Unit price XAF|Prix XAF","Amount XAF|Montant XAF");rows=cells(r,5);}
   case "DOC06","DOC07"->{String view=id.equals("DOC06")?"document_receipt_v1":"document_supplier_return_v1";r=required(query("business_date,description,base_unit,quantity_milli",""+view,"and id=:id",source,"line_id",5001));day=date(r.getFirst()[0]);columns=List.of("Product|Produit","Unit|Unité","Quantity milli|Quantité milli");rows=cells(r,1);}
   case "DOC08"->{r=required(query("party,business_date,source_id,kind,status,original_xaf,remaining_xaf","document_customer_statement_v1","and customer_id=:id",source,"business_date nulls last,source_id",5001));party=text(r.getFirst()[0]);status="CURRENT";columns=List.of("Date|Date","Source|Source","Balance kind|Type de solde","Status|État","Original XAF|Original XAF","Remaining XAF|Restant XAF");rows=cells(r,1);}
   case "DOC09","DOC11"->{r=required(query("business_date,kind,amount_xaf,customer_id,sale_id,deposit_id,journal_id,reason","document_tender_v1","and id=:id "+(id.equals("DOC09")?"and kind in('RECEIPT','DEPOSIT')":"and kind='REFUND'"),source,"id",1));var h=r.getFirst();day=date(h[0]);status=text(h[1]);total=text(h[2]);party=text(h[3]);columns=List.of("Amount XAF|Montant XAF","Customer|Client","Sale|Vente","Deposit|Dépôt","Journal|Journal","Reason|Motif");rows=cells(r,2);}
   case "DOC13","DOC24"->{r=required(query("business_date,status,version,description,base_unit,source_quantity_milli,output_quantity_milli,output_product,evidence_reference","document_inventory_job_v1","and id=:id and status='POSTED' "+(id.equals("DOC13")?"and kind='COUNT'":"and kind in('CUT','REPACK')"),source,"id",1));var h=r.getFirst();day=date(h[0]);status=text(h[1]);version=text(h[2]);columns=List.of("Source product|Produit source","Unit|Unité","Source milli|Source milli","Counted/output milli|Compté/sortie milli","Output product|Produit de sortie","Evidence|Justificatif");rows=cells(r,3);}
   case "DOC14"->{r=required(query("business_date,status,version,payee,amount_xaf,category,reason,paid_fact_id","document_expense_v1","and id=:id and status='POSTED'",source,"id",1));var h=r.getFirst();day=date(h[0]);status=text(h[1]);version=text(h[2]);party=text(h[3]);total=text(h[4]);columns=List.of("Amount XAF|Montant XAF","Category|Catégorie","Reason|Motif","Payment fact|Paiement");rows=cells(r,4);}
   case "DOC15"->{r=required(query("business_date,status,till_code,opening_xaf,movement_xaf,expected_xaf,counted_xaf,variance_xaf","report_cash_v1","and id=:id and status='CLOSED'",source,"id",1));day=date(r.getFirst()[0]);status=text(r.getFirst()[1]);columns=List.of("Till|Caisse","Opening XAF|Ouverture XAF","Movement XAF|Mouvement XAF","Expected XAF|Attendu XAF","Counted XAF|Compté XAF","Variance XAF|Écart XAF");rows=cells(r,2);}
   case "DOC16"->{r=required(query("business_date,status,version,description,base_unit,quantity_milli,received_milli,origin_warehouse,destination_warehouse","document_transfer_v1","and id=:id and status in('IN_TRANSIT','PARTIALLY_RECEIVED','RECEIVED')",source,"id",1));var h=r.getFirst();day=date(h[0]);status=text(h[1]);version=text(h[2]);columns=List.of("Product|Produit","Unit|Unité","Dispatched milli|Expédié milli","Received milli|Reçu milli","Origin|Origine","Destination|Destination");rows=cells(r,3);}
   case "DOC21"->{r=required(query("business_date,account_code,debit_xaf,credit_xaf,source_type,source_id","document_journal_v1","and id=:id",source,"line_id",5001));day=date(r.getFirst()[0]);columns=List.of("Account|Compte","Debit XAF|Débit XAF","Credit XAF|Crédit XAF","Source kind|Type source","Source|Source");rows=cells(r,1);}
   default->throw new NoSuchElementException();
  }
  return new Material(new Body(source.toString(),status,version,day,party,columns,rows,total,template.basis()),null);
 }
 private List<Object[]> required(List<Object[]> rows){if(rows.isEmpty())throw new NoSuchElementException("Scoped eligible source not found");if(rows.size()>5000)throw new IllegalStateException("Source exceeds 5000 capture rows; use a bounded report export");return rows;}
 private List<List<String>> cells(List<Object[]> rows,int start){return rows.stream().map(r->Arrays.stream(r).skip(start).map(DocumentSourceReader::text).toList()).toList();}
 private static LocalDate date(Object v){return v==null?null:LocalDate.parse(v.toString());}
 private static String text(Object v){return v==null?"":v instanceof java.math.BigDecimal d?d.toBigIntegerExact().toString():v.toString();}
}
