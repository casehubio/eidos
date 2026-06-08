package io.casehub.eidos.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vocabulary-level metadata for an enum implementing {@link VocabularyTerm}.
 * {@code name()}, {@code version()}, and {@code description()} default to {@code ""}
 * meaning "not provided"; callers should treat {@code isEmpty()} as absent.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VocabularyMetadata {
    String uri();
    String name()        default "";
    String version()     default "";
    String description() default "";
}
