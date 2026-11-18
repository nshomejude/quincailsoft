package com.quincaisoft.shared;
/** Integral XAF, one unit = one FCFA; no implicit rounding or mixed currencies. */
public record Money(long francs) {
 public Money add(Money other) { return new Money(Math.addExact(francs, other.francs)); }
 public Money subtract(Money other) { return new Money(Math.subtractExact(francs, other.francs)); }
 public Money multiply(long factor) { return new Money(Math.multiplyExact(francs, factor)); }
 public static Money parse(String amount) { if (!amount.matches("-?(0|[1-9][0-9]*)")) throw new IllegalArgumentException("Integral XAF string required"); return new Money(Long.parseLong(amount)); }
}
