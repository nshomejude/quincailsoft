package com.quincaisoft.purchasing;
import com.quincaisoft.purchasing.domain.PurchaseRules;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PurchaseRulesTest{
 @Test void exactFractionalQuantities(){assertEquals(500,PurchaseRules.amount(1250,400));}
 @Test void rejectsFractionalFcfa(){assertThrows(IllegalArgumentException.class,()->PurchaseRules.amount(1,1));}
 @Test void rejectsOverflow(){assertThrows(ArithmeticException.class,()->PurchaseRules.amount(2000,Long.MAX_VALUE));}
 @Test void partialReceiptConservesApprovedQuantity(){assertEquals(10000,PurchaseRules.receive(10000,4000,6000));assertThrows(IllegalArgumentException.class,()->PurchaseRules.receive(10000,4000,6001));}
 @Test void rejectsNoncanonicalValues(){for(String q:new String[]{"0","-1","1e3","1.5","+1"," 1"})assertThrows(IllegalArgumentException.class,()->PurchaseRules.positive(q));}
}
