package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FormationRulesDef {
    String[] allowedPaths() default {};
    int minGroupSize() default 0;
    int maxGroupSize() default 0;
    boolean allowSubGroups() default false;
    double proposalQuorum() default -1.0;
}
