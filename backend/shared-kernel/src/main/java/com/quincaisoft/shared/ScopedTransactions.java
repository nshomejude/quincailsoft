package com.quincaisoft.shared;
import java.util.function.Supplier;
public interface ScopedTransactions { <T> T execute(Supplier<T> operation); }
