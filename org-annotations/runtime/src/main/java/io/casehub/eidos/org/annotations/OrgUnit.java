package io.casehub.eidos.org.annotations;

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
}
