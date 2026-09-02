package io.casehub.eidos.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AgentTemplates.class)
public @interface AgentTemplateRef {
    String id();
    TemplateArg[] args() default {};
}
