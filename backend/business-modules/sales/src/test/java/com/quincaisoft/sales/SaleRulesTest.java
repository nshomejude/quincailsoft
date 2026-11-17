package com.quincaisoft.sales;
import com.quincaisoft.sales.domain.SaleRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SaleRulesTest {
 @Test void supportsFractionalUnitsWhenXafIsExact(){assertEquals(250,SaleRules.amount(1250,200));}
 @Test void rejectsFractionalFcfa(){assertThrows(IllegalArgumentException.class,()->SaleRules.amount(1,1));}
 @Test void rejectsOverflowWithoutTruncation(){assertThrows(ArithmeticException.class,()->SaleRules.amount(2000,Long.MAX_VALUE));}
 @Test void rejectsExponentialSignedAndFractionalQuantities(){for(var s:new String[]{"1e3","+1000","-1000","1.5","0"," 1000"})assertThrows(IllegalArgumentException.class,()->SaleRules.quantity(s));}
 @Test void zeroOrNegativePriceRejected(){assertThrows(IllegalArgumentException.class,()->SaleRules.amount(1000,0));assertThrows(IllegalArgumentException.class,()->SaleRules.amount(1000,-1));}
 @Test void exactMaxDoesNotOverflowIntermediate(){assertEquals(Long.MAX_VALUE,SaleRules.amount(1000,Long.MAX_VALUE));}
}
