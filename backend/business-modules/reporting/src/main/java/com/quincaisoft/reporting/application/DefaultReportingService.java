package com.quincaisoft.reporting.application;
import com.quincaisoft.reporting.api.ReportingService;import com.quincaisoft.reporting.domain.ReportRules;import com.quincaisoft.shared.*;
import org.jooq.*;import org.jooq.impl.DSL;import org.springframework.stereotype.Service;import java.time.*;import java.util.*;import java.math.BigDecimal;
@Service public class DefaultReportingService implements ReportingService {
 private final DSLContext sql;private final ScopedTransactions tx;
 public DefaultReportingService(DSLContext sql,ScopedTransactions tx){this.sql=sql;this.tx=tx;}
 private record Spec(String view,String title,String basis,List<String> columns,List<String> totals,String order,boolean dated){}
 private static final Map<String,Spec> SPECS=Map.of(
 "posted-sales",new Spec("report_posted_sales_v1","Posted sales and recognized cost","Posted sales by business date; amounts exclude subsequent returns",List.of("id","business_date","customer_id","status","total_xaf","cost_xaf","margin_xaf"),List.of("total_xaf","cost_xaf","margin_xaf"),"id",true),
 "stock",new Spec("report_stock_v1","Merchant serviceable stock","Current merchant-serviceable positions only; not full physical/custody inventory",List.of("warehouse_id","product_id","sku","name","base_unit","quantity_milli","reserved_milli","allocated_milli","available_milli","value_xaf"),List.of("value_xaf"),"product_id",false),
 "receivables",new Spec("report_receivable_v1","Outstanding customer sales","Current per-sale receivable projection; not customer credit/deposit liabilities or aging",List.of("sale_id","customer_id","business_date","outstanding_xaf"),List.of("outstanding_xaf"),"sale_id",false),
 "cash",new Spec("report_cash_v1","Cash session reconciliation","Session business date; expected cash equals opening plus immutable cash movements",List.of("id","till_code","business_date","status","opening_xaf","movement_xaf","expected_xaf","counted_xaf","variance_xaf"),List.of("opening_xaf","movement_xaf","expected_xaf"),"id",true),
 "provider-intents",new Spec("report_provider_intent_v1","Provider intent status","Intent amounts are requests, not collected money; sandbox EUR and live XAF are never summed",List.of("id","sale_id","business_date","environment","currency","status","amount","journal_id"),List.of(),"id",true));
 public List<Definition> catalog(){return SPECS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->new Definition(e.getKey(),e.getValue().title,e.getValue().basis,e.getValue().columns)).toList();}
 public Report run(String code,LocalDate from,LocalDate to,int offset,int limit){ReportRules.page(offset,limit);Spec spec=SPECS.get(code);if(spec==null)throw new NoSuchElementException("Unknown implemented report code");if(spec.dated)ReportRules.range(from,to);else if(from!=null||to!=null)throw new IllegalArgumentException("This current-position report does not accept historical dates");
  return tx.execute(()->{var scope=TenantContext.current();Condition predicate=DSL.field("tenant_id",UUID.class).eq(scope.tenantId()).and(DSL.field("company_id",UUID.class).eq(scope.companyId())).and(DSL.field("branch_id",UUID.class).eq(scope.branchId()));if(spec.dated)predicate=predicate.and(DSL.field("business_date",LocalDate.class).between(from,to));
   var fields=new ArrayList<SelectFieldOrAsterisk>();spec.columns.forEach(c->fields.add(DSL.field(DSL.name(c))));spec.totals.forEach(c->fields.add(DSL.sum(DSL.field(DSL.name(c),BigDecimal.class)).over().as("__total_"+c)));var fetched=sql.select(fields).from(DSL.name(spec.view)).where(predicate).orderBy(DSL.field(DSL.name(spec.order)),DSL.field(DSL.name(spec.columns.getFirst()))).limit(limit+1).offset(offset).fetch();boolean more=fetched.size()>limit;
   List<List<String>> rows=fetched.stream().limit(limit).map(row->spec.columns.stream().map(c->exact(row.get(c))).toList()).toList();var totals=new LinkedHashMap<String,String>();
   if(!fetched.isEmpty())for(String column:spec.totals){BigDecimal sum=fetched.getFirst().get("__total_"+column,BigDecimal.class);totals.put(column,sum==null?"0":sum.toBigIntegerExact().toString());}
   return new Report(code,spec.basis,scope.tenantId(),scope.companyId(),scope.branchId(),from,to,Instant.now(),spec.columns,rows,Map.copyOf(totals),offset,limit,more);
  });}
 private static String exact(Object value){if(value==null)return "";if(value instanceof BigDecimal n)return n.toBigIntegerExact().toString();return value.toString();}
 public String csv(Report report){var lines=new ArrayList<String>();lines.add(report.columns().stream().map(ReportRules::cell).collect(java.util.stream.Collectors.joining(",")));for(var row:report.rows())lines.add(row.stream().map(ReportRules::cell).collect(java.util.stream.Collectors.joining(",")));return String.join("\r\n",lines)+"\r\n";}
}
