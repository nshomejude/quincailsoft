package com.quincaisoft.shared;
import java.util.concurrent.Callable;
/** Immutable lexical request binding. Never inherited by pooled/background workers. */
public final class TenantContext {
 private record Value(TenantScope scope,long epoch) {}
 private static final ScopedValue<Value> CURRENT=ScopedValue.newInstance();
 private TenantContext(){}
 public static TenantScope current(){if(!CURRENT.isBound())throw new IllegalStateException("Authenticated tenant scope required");return CURRENT.get().scope();}
 public static long authorizationEpoch(){return CURRENT.get().epoch();}
 public static <T> T call(TenantScope scope,long epoch,Callable<T> action) throws Exception {return ScopedValue.where(CURRENT,new Value(scope,epoch)).call(action::call);}
}
