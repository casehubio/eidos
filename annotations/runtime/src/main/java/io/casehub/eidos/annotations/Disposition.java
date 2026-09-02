package io.casehub.eidos.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Disposition {
    String socialOrient() default "";

    String ruleFollowing() default "";

    String riskAppetite() default "";

    String autonomy() default "";

    String conflictMode() default "";

    boolean delegation() default false;

    DispositionWeight[] dispositionProfile() default {};

    DispositionWeight[] styleProfile() default {};

    String mbtiType() default "";

    String enneagramType() default "";

    AxisVocabulary[] axisVocabularies() default {};
}
