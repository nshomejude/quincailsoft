package com.quincaisoft.reporting;
import com.quincaisoft.reporting.domain.ReportRules;import org.junit.jupiter.api.Test;import java.time.LocalDate;import static org.junit.jupiter.api.Assertions.*;
class ReportRulesTest {
 @Test void spreadsheetFormulasCannotExecute(){for(String text:new String[]{"=1+1"," +cmd","-1","@SUM(A1)"})assertTrue(ReportRules.cell(text).startsWith("\"'"));assertEquals("\"normal, \"\"quoted\"\"\"",ReportRules.cell("normal, \"quoted\""));}
 @Test void invalidRangesAndUnboundedPagesFail(){assertThrows(IllegalArgumentException.class,()->ReportRules.range(null,null));assertThrows(IllegalArgumentException.class,()->ReportRules.range(LocalDate.of(2026,1,1),LocalDate.of(2028,1,1)));assertThrows(IllegalArgumentException.class,()->ReportRules.page(-1,100));assertThrows(IllegalArgumentException.class,()->ReportRules.page(0,1001));}
}
