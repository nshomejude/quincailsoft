package com.quincaisoft.shared;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ValueObjectsTest {
 @Test void moneyNeverSilentlyRoundsOrOverflows(){assertThrows(IllegalArgumentException.class,()->Money.parse("1.5"));assertThrows(ArithmeticException.class,()->new Money(Long.MAX_VALUE).add(new Money(1)));assertEquals(-5,new Money(5).subtract(new Money(10)).francs());}
 @Test void quantitiesRequireExactRationalConversion(){assertEquals(1500,new Quantity(1000).convert(3,2).milliUnits());assertThrows(IllegalArgumentException.class,()->new Quantity(1).convert(1,3));assertThrows(ArithmeticException.class,()->new Quantity(Long.MAX_VALUE).convert(2,1));}
 @Test void absentTenantFailsClosed(){assertThrows(IllegalStateException.class,TenantContext::current);}
}
