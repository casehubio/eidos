package io.casehub.eidos.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AgentCapabilities.class)
public @interface AgentCapabilityDef {
    String name();
    String description() default "";
    String capabilityVocabulary() default "";
    double qualityHint() default -1;
    long latencyHintP50Ms() default -1;
    String costHint() default "";
    String[] inputTypes() default {};
    String[] outputTypes() default {};
    String[] tags() default {};
    EpistemicDomain[] epistemicDomains() default {};
    String[] excludedDomains() default {};
}
