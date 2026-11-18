package com.quincaisoft.sales.domain;
import java.math.BigInteger;
public final class SaleRules {
 private SaleRules() {}
 public static long amount(long quantityMilli,long priceXaf){
  if(quantityMilli<=0||priceXaf<=0)throw new IllegalArgumentException("Positive quantity and catalog price required");
  var value=BigInteger.valueOf(quantityMilli).multiply(BigInteger.valueOf(priceXaf)).divideAndRemainder(BigInteger.valueOf(1000));
  if(value[1].signum()!=0)throw new IllegalArgumentException("Fractional FCFA requires an approved rounding policy");
  return value[0].longValueExact();
 }
 public static long quantity(String text){
  if(text==null||!text.matches("[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Quantity must be a positive integer milli-unit string");
  return Long.parseLong(text);
 }
}
