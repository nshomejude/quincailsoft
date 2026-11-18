package com.quincaisoft.reporting.web;
import com.quincaisoft.reporting.api.ReportingService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.http.*;import java.time.LocalDate;import java.util.List;
@RestController @RequestMapping("/api/v1/reports") @PreAuthorize("hasAnyAuthority('reports:read','SCOPE_reports:read')")
public class ReportingController {
 private final ReportingService reports;public ReportingController(ReportingService reports){this.reports=reports;}
 @GetMapping("/catalog") public List<ReportingService.Definition> catalog(){return reports.catalog();}
 @GetMapping("/{code}") public ReportingService.Report run(@PathVariable String code,@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="100")int limit){return reports.run(code,from,to,offset,limit);}
 @GetMapping(value="/{code}/export",produces="text/csv") @PreAuthorize("hasAnyAuthority('reports:export','SCOPE_reports:export')") public ResponseEntity<String> export(@PathVariable String code,@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="1000")int limit){var report=reports.run(code,from,to,offset,limit);return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=quincai-"+report.code()+"-page-"+offset+".csv").header("X-Report-Has-More",Boolean.toString(report.hasMore())).body(reports.csv(report));}
}
