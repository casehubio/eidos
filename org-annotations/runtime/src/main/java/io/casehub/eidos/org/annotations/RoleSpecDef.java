package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RoleSpecDef {
    String roleName();
    String roleVocabulary() default "";
    int minCardinality() default 0;
    int maxCardinality() default 0;
    String[] incompatibleWith() default {};
    RolePolicyDef[] policies() default {};
}
