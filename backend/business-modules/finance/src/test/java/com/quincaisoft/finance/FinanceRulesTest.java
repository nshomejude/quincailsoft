package com.quincaisoft.finance;
import com.quincaisoft.finance.api.FinanceService;
import org.junit.jupiter.api.Test;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FinanceRulesTest {
 @Test void rejectsUnbalancedJournal(){assertThrows(IllegalArgumentException.class,()->FinanceService.balanced(List.of(new FinanceService.Entry("AR",100,0),new FinanceService.Entry("SALES",0,99))));}
 @Test void acceptsBalancedPosting(){assertDoesNotThrow(()->FinanceService.balanced(List.of(new FinanceService.Entry("AR",100,0),new FinanceService.Entry("SALES",0,100))));}
 @Test void rejectsBothSidesAndBlankAccount(){assertThrows(IllegalArgumentException.class,()->new FinanceService.Entry("AR",100,100));assertThrows(IllegalArgumentException.class,()->new FinanceService.Entry("",100,0));}
 @Test void totalOverflowFails(){assertThrows(ArithmeticException.class,()->FinanceService.balanced(List.of(new FinanceService.Entry("AR",Long.MAX_VALUE,0),new FinanceService.Entry("AR",1,0),new FinanceService.Entry("SALES",0,1))));}
}
