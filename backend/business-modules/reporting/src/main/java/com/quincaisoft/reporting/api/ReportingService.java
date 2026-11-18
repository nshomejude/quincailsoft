package com.quincaisoft.reporting.api;
import java.time.*;import java.util.*;
public interface ReportingService {
 record Definition(String code,String title,String basis,List<String> columns,boolean requiresDateRange,String permission){}
 record Report(String code,String basis,UUID tenantId,UUID companyId,UUID branchId,LocalDate from,LocalDate to,Instant generatedAt,List<String> columns,List<List<String>> rows,Map<String,String> totals,int offset,int limit,boolean hasMore){}
 List<Definition> catalog();Report run(String code,LocalDate from,LocalDate to,int offset,int limit);String csv(Report report);
}
