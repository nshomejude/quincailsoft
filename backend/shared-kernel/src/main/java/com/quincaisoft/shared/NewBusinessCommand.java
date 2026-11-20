package com.quincaisoft.shared;
import java.lang.annotation.*;
/** Billing gate for new commitments; does not apply to history, returns or reconciliation. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface NewBusinessCommand {}
