package io.casehub.eidos.annotations.deployment;

import org.jboss.jandex.AnnotationInstance;

public final class AnnotationProcessorUtils {

    private AnnotationProcessorUtils() {}

    public static String stringValue(AnnotationInstance ann, String key) {
        var v = ann.value(key);
        return v != null ? v.asString() : "";
    }

    public static String enumValue(AnnotationInstance ann, String key, String defaultValue) {
        var v = ann.value(key);
        return v != null ? v.asEnum() : defaultValue;
    }
}
