package com.quincaisoft.inventory;
import com.quincaisoft.inventory.infrastructure.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StockBalanceTest {
 @Test void availableExcludesReservationsAndExclusiveOfflineAllocation(){var b=new StockBalance();b.receive(10000,5000);b.reservedMilli=3000;b.allocatedMilli=2000;assertEquals(2500,b.issue(5000));assertEquals(5000,b.quantityMilli);assertThrows(IllegalArgumentException.class,()->b.issue(1));assertEquals(2500,b.valueXaf);}
 @Test void fullIssueConservesAllValue(){var b=new StockBalance();b.receive(3000,100);assertEquals(100,b.issue(3000));assertEquals(0,b.quantityMilli);assertEquals(0,b.valueXaf);}
 @Test void unsupportedFractionalFrancDoesNotMutate(){var b=new StockBalance();b.receive(3000,100);assertThrows(IllegalArgumentException.class,()->b.issue(1000));assertEquals(3000,b.quantityMilli);assertEquals(100,b.valueXaf);}
 @Test void largeWeightedIntermediateDoesNotOverflow(){assertEquals(Long.MAX_VALUE,StockCost.exactIssue(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE));}
 @Test void overIssueAndInvalidInputsRejected(){var b=new StockBalance();b.receive(2000,100);assertThrows(IllegalArgumentException.class,()->b.issue(2001));assertThrows(IllegalArgumentException.class,()->b.issue(-1));assertThrows(IllegalArgumentException.class,()->b.receive(1,-1));assertEquals(2000,b.quantityMilli);}
 @Test void valueOverflowDoesNotPartiallyMutate(){var b=new StockBalance();b.receive(1,Long.MAX_VALUE);assertThrows(ArithmeticException.class,()->b.receive(1,1));assertEquals(1,b.quantityMilli);assertEquals(Long.MAX_VALUE,b.valueXaf);}
 @Test void quantityOverflowRejected(){var b=new StockBalance();b.receive(Long.MAX_VALUE,1);assertThrows(ArithmeticException.class,()->b.receive(1,1));assertEquals(Long.MAX_VALUE,b.quantityMilli);}
}
