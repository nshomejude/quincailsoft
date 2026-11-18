package com.quincaisoft.shared;
import java.math.BigInteger;
/** Exact integer thousandths of the SKU base unit. */
public record Quantity(long milliUnits) {
 public Quantity add(Quantity other) { return new Quantity(Math.addExact(milliUnits,other.milliUnits)); }
 public Quantity subtract(Quantity other) { return new Quantity(Math.subtractExact(milliUnits,other.milliUnits)); }
 public Quantity convert(long numerator,long denominator) {
  if(numerator<=0 || denominator<=0) throw new IllegalArgumentException("Positive conversion ratio required");
  var result=BigInteger.valueOf(milliUnits).multiply(BigInteger.valueOf(numerator)).divideAndRemainder(BigInteger.valueOf(denominator));
  if(result[1].signum()!=0) throw new IllegalArgumentException("Conversion is not exactly representable");
  return new Quantity(result[0].longValueExact());
 }
}
