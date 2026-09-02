package io.casehub.eidos.annotations;

import io.casehub.eidos.api.DispositionAxis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AxisVocabulary {
    DispositionAxis axis();
    String uri();
}
