package io.casehub.eidos.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EpistemicDomain {
    String value();
    double score();
}
