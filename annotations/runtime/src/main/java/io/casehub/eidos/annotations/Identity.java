package io.casehub.eidos.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Identity {
    String id() default "";
    String name() default "";
    String slot();
    String provider() default "";
    String modelFamily() default "";
    String jurisdiction() default "";
    String dataHandlingPolicy() default "";
    String briefing() default "";
    String vocabulary() default "";
    String slotVocabulary() default "";
    String dispositionVocabulary() default "";
    String styleVocabulary() default "";
    String version() default "";
}
