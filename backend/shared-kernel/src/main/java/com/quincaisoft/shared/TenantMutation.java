package com.quincaisoft.shared;
import java.lang.annotation.*;
/** New merchant mutation subject to tenant security suspension and resumed-epoch fencing. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface TenantMutation {}
