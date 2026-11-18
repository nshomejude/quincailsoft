package com.quincaisoft.inventory.infrastructure;
import com.quincaisoft.inventory.api.StockService;
import java.math.BigInteger;
/** Policy WAC_HALF_EVEN_V1: allocate integer XAF from the authoritative pool;
 * the final issue always clears the remaining value. BigInteger prevents intermediate overflow. */
public final class StockCost {
 public static long exactIssue(long value,long quantity,long total){
  if(value<0||quantity<=0||total<=0||quantity>total)throw new IllegalArgumentException("Invalid stock valuation inputs");
  var parts=BigInteger.valueOf(value).multiply(BigInteger.valueOf(quantity)).divideAndRemainder(BigInteger.valueOf(total));
  int half=parts[1].shiftLeft(1).compareTo(BigInteger.valueOf(total));
  return (half>0 || half==0 && parts[0].testBit(0) ? parts[0].add(BigInteger.ONE):parts[0]).longValueExact();
 }
}
