package com.quincaisoft.inventory.infrastructure;
import com.quincaisoft.inventory.api.StockService;
import java.math.BigInteger;
/** Exact integral-XAF WAC slice; configured remainder policy remains a release gate. */
public final class StockCost {
 public static long exactIssue(long value,long quantity,long total){
  if(value<0||quantity<=0||total<=0||quantity>total)throw new IllegalArgumentException("Invalid stock valuation inputs");
  var parts=BigInteger.valueOf(value).multiply(BigInteger.valueOf(quantity)).divideAndRemainder(BigInteger.valueOf(total));
  if(parts[1].signum()!=0)throw new IllegalArgumentException("Issue cost requires a fractional FCFA; configured valuation rounding is required");
  return parts[0].longValueExact();
 }
}
