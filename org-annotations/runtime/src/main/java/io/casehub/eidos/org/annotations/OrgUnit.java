package io.casehub.eidos.org.annotations;

import io.casehub.eidos.annotations.AgentCapabilityDef;
import io.casehub.eidos.annotations.AgentConstraintDef;
import io.casehub.eidos.annotations.AgentGoalDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OrgUnit {
    String id() default "";
    String name() default "";
    String kind() default "";
    String kindVocabulary() default "";
    String parentUnit() default "";

    AgentCapabilityDef[] capabilities() default {};
    AgentGoalDef[] goals() default {};
    AgentConstraintDef[] constraints() default {};
}
