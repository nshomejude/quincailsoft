package com.quincaisoft.purchasing.domain;
import java.math.BigInteger;
public final class PurchaseRules {private PurchaseRules(){}
 public static long positive(String value){if(value==null||!value.matches("[1-9][0-9]*"))throw new IllegalArgumentException("Positive integer string required");return Long.parseLong(value);}
 public static long amount(long quantity,long price){if(quantity<=0||price<=0)throw new IllegalArgumentException("Positive quantity and price required");var v=BigInteger.valueOf(quantity).multiply(BigInteger.valueOf(price)).divideAndRemainder(BigInteger.valueOf(1000));if(v[1].signum()!=0)throw new IllegalArgumentException("Fractional FCFA requires configured rounding policy");return v[0].longValueExact();}
 public static long receive(long ordered,long prior,long incoming){if(incoming<=0)throw new IllegalArgumentException("Positive receipt required");long total=Math.addExact(prior,incoming);if(total>ordered)throw new IllegalArgumentException("Receipt exceeds approved order quantity");return total;}
 public static void reason(String v){if(v==null||v.isBlank()||v.length()>2000)throw new IllegalArgumentException("Reason required, maximum 2000 characters");}
}