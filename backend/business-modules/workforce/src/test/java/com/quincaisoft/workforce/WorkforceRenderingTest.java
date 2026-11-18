package com.quincaisoft.workforce;
import com.quincaisoft.workforce.api.WorkforceService;
import com.quincaisoft.workforce.application.WorkforceRendering;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WorkforceRenderingTest {
 @Test void payslipEscapesUntrustedSnapshotAndExportNeutralizesFormulas(){
  var day=LocalDate.of(2024,1,1);var p=new WorkforceService.Payslip(UUID.randomUUID(),UUID.randomUUID(),"=HYPERLINK(\"evil\")<script>alert(1)</script>","@EMP",day,day,day,"9007199254740993","1","9007199254740992","<img src=x onerror=alert(1)>","ACCRUED_UNPAID",UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"1");
  var html=WorkforceRendering.html(p,List.of(new WorkforceService.Deduction("X","<svg/onload=x>","1","manual & reviewed")));
  assertFalse(html.contains("<script>"));assertFalse(html.contains("<svg/"));assertFalse(html.contains("<img src"));assertTrue(html.contains("&lt;script&gt;"));assertTrue(html.contains("manual &amp; reviewed"));assertTrue(html.contains("9007199254740993"));
  var csv=WorkforceRendering.csv(List.of(p));assertTrue(csv.contains("\"'@EMP\""));assertTrue(csv.contains("\"'=HYPERLINK("));assertTrue(csv.contains("\"9007199254740993\""));
 }
}
