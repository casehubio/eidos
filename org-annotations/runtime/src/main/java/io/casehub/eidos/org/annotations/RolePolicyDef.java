package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RolePolicyDef {
    String capabilityName();
    String kind();
    PolicyExpectation[] expectations() default {};
}
