package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface GroupPolicyDef {
    int normativePriority() default -1;
    String normSpecificity() default "";
    FormationRulesDef formationRules() default @FormationRulesDef;
}
