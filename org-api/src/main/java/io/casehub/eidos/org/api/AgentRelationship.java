package io.casehub.eidos.org.api;

import java.util.Objects;

public record AgentRelationship(
    String sourceAgentId,
    String targetAgentId,
    RelationshipKind kind,
    String extendedKind,
    String kindVocabulary,
    RelationshipScope scope,
    AttestationGrant attestation,
    String tenancyId
) {
    public AgentRelationship {
        Objects.requireNonNull(sourceAgentId, "sourceAgentId");
        Objects.requireNonNull(targetAgentId, "targetAgentId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tenancyId, "tenancyId");
        if (sourceAgentId.equals(targetAgentId)) {
            throw new OrgValidationException("relationship", "self-referential relationship");
        }
        if (kind == RelationshipKind.EXTENDED && (extendedKind == null || extendedKind.isBlank())) {
            throw new OrgValidationException("extendedKind",
                "required when kind is EXTENDED");
        }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String sourceAgentId, targetAgentId, extendedKind, kindVocabulary, tenancyId;
        private RelationshipKind kind;
        private RelationshipScope scope;
        private AttestationGrant attestation;

        public Builder sourceAgentId(String v) { this.sourceAgentId = v; return this; }
        public Builder targetAgentId(String v) { this.targetAgentId = v; return this; }
        public Builder kind(RelationshipKind v) { this.kind = v; return this; }
        public Builder extendedKind(String v) { this.extendedKind = v; return this; }
        public Builder kindVocabulary(String v) { this.kindVocabulary = v; return this; }
        public Builder scope(RelationshipScope v) { this.scope = v; return this; }
        public Builder attestation(AttestationGrant v) { this.attestation = v; return this; }
        public Builder tenancyId(String v) { this.tenancyId = v; return this; }

        public AgentRelationship build() {
            return new AgentRelationship(sourceAgentId, targetAgentId, kind,
                extendedKind, kindVocabulary, scope, attestation, tenancyId);
        }
    }
}
