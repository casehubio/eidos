package io.casehub.eidos.annotations;

import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.Visibility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AgentConstraints.class)
public @interface AgentConstraintDef {
    String name();
    String description();
    ConstraintSeverity severity() default ConstraintSeverity.HARD;
    Visibility visibility() default Visibility.PUBLIC;
}
