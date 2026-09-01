package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OrgMemberDef {
    String agentId();
    String role() default "";
    String roleVocabulary() default "";
}
