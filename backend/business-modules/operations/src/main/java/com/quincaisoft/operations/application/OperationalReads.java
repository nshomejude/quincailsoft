package com.quincaisoft.operations.application;

import com.quincaisoft.operations.api.OperationsService.*;
import com.quincaisoft.operations.infrastructure.OperationReceipts;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** Fixed projections only. No caller-selected SQL, table, predicate or secret-bearing export. */
@Component public class OperationalReads {
 private final EntityManager em;private final JsonMapper json;
 public OperationalReads(EntityManager em,JsonMapper json){this.em=em;this.json=json;}
 private record ExportProjection(String table,String columns,List<String> headers){}
 private static final Map<Entity,ExportProjection> EXPORTS=Map.of(
  Entity.PRODUCT,new ExportProjection("catalog_product","sku,name,base_unit,selling_price_xaf,tax_basis_points,brand,barcode,pack_size_milli,reorder_level_milli,track_stock,track_serial,track_expiry,description,active",List.of("sku","name","base_unit","selling_price_xaf","tax_basis_points","brand","barcode","pack_size_milli","reorder_level_milli","track_stock","track_serial","track_expiry","description","active")),
  Entity.CUSTOMER,new ExportProjection("customer","name,customer_type,phone,email,address,tax_reference,credit_limit_xaf,payment_terms_days,consent,active",List.of("name","type","phone","email","address","tax_reference","credit_limit_xaf","payment_terms_days","consent","active")),
  Entity.SUPPLIER,new ExportProjection("supplier","code,name,contact_person,phone,email,address,tax_reference,payment_terms_days,bank_reference,active",List.of("code","name","contact_person","phone","email","address","tax_reference","payment_terms_days","bank_reference","active")),
  Entity.CATEGORY,new ExportProjection("catalog_category","code,name,parent_id,brand,description,active",List.of("code","name","parent_id","brand","description","active")),
  Entity.UNIT,new ExportProjection("catalog_unit","code,name,base_unit,factor_numerator,factor_denominator,decimal_precision,active",List.of("code","name","base_unit","factor_numerator","factor_denominator","precision","active")));
 public record Rendered(String content,int rows){}
 public Rendered csv(Entity entity){var p=EXPORTS.get(Objects.requireNonNull(entity));var rows=em.createNativeQuery("select "+p.columns()+" from "+p.table()+" where tenant_id=:t and company_id=:c order by id limit 5001").setParameter("t",com.quincaisoft.shared.TenantContext.current().tenantId()).setParameter("c",com.quincaisoft.shared.TenantContext.current().companyId()).getResultList();if(rows.size()>5000)throw new IllegalArgumentException("Export exceeds bounded 5000-row job; partition before retrying");var result=new StringBuilder(Csv.row(p.headers()));int bytes=result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;for(Object item:rows){String row=Csv.row(Arrays.asList((Object[])item));bytes+=row.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;if(bytes>2_000_000)throw new IllegalArgumentException("Export exceeds bounded 2 MB job");result.append(row);}return new Rendered(result.toString(),rows.size());}
 private static final Map<String,String> SECTIONS;
 static {
  var sources=new TreeMap<String,String>();
  for(String table:List.of("company","branch","catalog_product","catalog_category","catalog_unit","customer","supplier","warehouse","stock_balance","stock_movement","sale","journal_entry","tender_fact","provider_intent","document_snapshot","purchase_order","purchase_receipt","supplier_invoice"))sources.put(table,"select t.* from "+table+" t");
  sources.put("sale_line","select t.* from sale_line t join sale s on s.tenant_id=t.tenant_id and s.company_id=t.company_id and s.id=t.sale_id");
  sources.put("journal_line","select t.* from journal_line t join journal_entry j on j.tenant_id=t.tenant_id and j.company_id=t.company_id and j.id=t.entry_id");
  SECTIONS=Collections.unmodifiableMap(sources);
 }
 /** A single SQL statement gives every section the same PostgreSQL statement snapshot. */
 public List<Section> sections(){
  String sql=SECTIONS.entrySet().stream().map(entry->"select '"+entry.getKey()+"' as section,count(*) as rows,encode(sha256(convert_to(coalesce(string_agg(encode(sha256(convert_to(to_jsonb(bounded)::text,'UTF8')),'hex'),'' order by to_jsonb(bounded)::text),''),'UTF8')),'hex') as digest from ("+entry.getValue()+" limit 5001) bounded").collect(java.util.stream.Collectors.joining(" union all "))+" order by section";
  List<?> raw=em.createNativeQuery(sql).getResultList();var result=new ArrayList<Section>();for(Object item:raw){Object[] r=(Object[])item;long count=((Number)r[1]).longValue();if(count>5000)throw new IllegalStateException("Manifest section exceeds 5000-row preflight limit; isolated recovery tooling required");result.add(new Section((String)r[0],count,(String)r[2]));}return List.copyOf(result);
 }
 public String digest(List<Section> sections){return OperationReceipts.digest(json.writeValueAsString(sections));}
 public boolean balancedJournals(){Number count=(Number)em.createNativeQuery("select count(*) from (select j.id from journal_entry j left join journal_line l on l.tenant_id=j.tenant_id and l.company_id=j.company_id and l.entry_id=j.id group by j.id having count(l.id)<2 or coalesce(sum(l.debit_xaf::numeric-l.credit_xaf::numeric),0)<>0) bad").getSingleResult();return count.longValue()==0;}
}
