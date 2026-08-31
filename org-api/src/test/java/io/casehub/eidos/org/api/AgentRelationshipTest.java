package io.casehub.eidos.org.api;

import io.casehub.eidos.api.BehavioralSignal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRelationshipTest {

    @Test void minimalRelationship() {
        var rel = AgentRelationship.builder()
            .sourceAgentId("witness-1").targetAgentId("polecat-1")
            .kind(RelationshipKind.SUPERVISES).tenancyId("gastown").build();
        assertThat(rel.sourceAgentId()).isEqualTo("witness-1");
        assertThat(rel.targetAgentId()).isEqualTo("polecat-1");
        assertThat(rel.kind()).isEqualTo(RelationshipKind.SUPERVISES);
        assertThat(rel.scope()).isNull();
        assertThat(rel.attestation()).isNull();
    }

    @Test void rejectsSelfReferential() {
        assertThatThrownBy(() -> AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("a")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t").build())
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("self-referential");
    }

    @Test void extendedRequiresExtendedKind() {
        assertThatThrownBy(() -> AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.EXTENDED).tenancyId("t").build())
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("extendedKind");
    }

    @Test void extendedWithBlankKindRejected() {
        assertThatThrownBy(() -> AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.EXTENDED).extendedKind("  ").tenancyId("t").build())
            .isInstanceOf(OrgValidationException.class);
    }

    @Test void validExtendedRelationship() {
        var rel = AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.EXTENDED).extendedKind("monitors")
            .kindVocabulary("urn:gastown:vocab:org").tenancyId("t").build();
        assertThat(rel.extendedKind()).isEqualTo("monitors");
        assertThat(rel.kindVocabulary()).isEqualTo("urn:gastown:vocab:org");
    }

    @Test void scopedRelationship() {
        var rel = AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t")
            .scope(new RelationshipScope("code-review", null, null)).build();
        assertThat(rel.scope().capabilityName()).isEqualTo("code-review");
    }

    @Test void withAttestationGrant() {
        var grant = new AttestationGrant(
            Set.of("LATENCY"), Set.of("code-review"),
            Set.of(BehavioralSignal.VIOLATED));
        var rel = AgentRelationship.builder()
            .sourceAgentId("a").targetAgentId("b")
            .kind(RelationshipKind.SUPERVISES).tenancyId("t")
            .attestation(grant).build();
        assertThat(rel.attestation().dimensions()).containsExactly("LATENCY");
    }

    @Test void allRelationshipKinds() {
        for (var kind : RelationshipKind.values()) {
            if (kind == RelationshipKind.EXTENDED) continue;
            var rel = AgentRelationship.builder()
                .sourceAgentId("a").targetAgentId("b")
                .kind(kind).tenancyId("t").build();
            assertThat(rel.kind()).isEqualTo(kind);
        }
    }
}
