package io.casehub.eidos.org.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OrgRelationshipDef {
    String source();
    String target();
    String kind();
    String extendedKind() default "";
    String kindVocabulary() default "";
    String scope() default "";
    String scopeDomain() default "";
    String scopeCondition() default "";
    AttestationGrantDef[] attestation() default {};
}
