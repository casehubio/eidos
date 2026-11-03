package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PolicyExpectation {
    String key();
    String value();
}
