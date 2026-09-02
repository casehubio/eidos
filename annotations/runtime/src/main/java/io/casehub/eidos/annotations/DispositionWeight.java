package io.casehub.eidos.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DispositionWeight {
    String value();
    double weight() default 1.0;
}
