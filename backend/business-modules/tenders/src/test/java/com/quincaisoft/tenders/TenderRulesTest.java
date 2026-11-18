package com.quincaisoft.tenders;
import com.quincaisoft.tenders.domain.TenderRules;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class TenderRulesTest {
 @Test void exactXaf(){assertEquals(Long.MAX_VALUE,TenderRules.amount("9223372036854775807",false));assertEquals(0,TenderRules.amount("0",true));}
 @Test void ambiguousAmountsRejected(){for(var s:new String[]{"1e3","1.0","+10","-1","01"," 1","9223372036854775808"})assertThrows(IllegalArgumentException.class,()->TenderRules.amount(s,false));}
 @Test void conservation(){assertEquals(1000,TenderRules.remaining(10000,7000,2000));assertThrows(IllegalStateException.class,()->TenderRules.remaining(1000,800,300));}
}
