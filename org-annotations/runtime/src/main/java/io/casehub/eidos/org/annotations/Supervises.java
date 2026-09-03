package io.casehub.eidos.org.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Supervisions.class)
public @interface Supervises {
    String source();

    String target();

    String scope() default "";
    String scopeDomain() default "";
    String scopeCondition() default "";
}
