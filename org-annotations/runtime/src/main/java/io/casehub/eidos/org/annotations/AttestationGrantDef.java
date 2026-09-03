package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface AttestationGrantDef {
    String[] dimensions();
    String[] capabilityScope() default {};
    String[] signalTypes() default {};
}
