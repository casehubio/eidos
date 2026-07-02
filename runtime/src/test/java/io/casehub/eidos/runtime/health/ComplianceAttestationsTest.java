package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.ComplianceDimension;
import io.casehub.ledger.api.model.AttestationVerdict;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceAttestationsTest {

    @Test
    void violation_creates_flagged_attestation() {
        var entryId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();
        var att = ComplianceAttestations.violation(entryId, subjectId,
                "code-review", "latency", "28500ms exceeded 5000ms p50", 0.0);

        assertThat(att.attestorId).isEqualTo(ComplianceDimension.ATTESTOR_ID);
        assertThat(att.attestorType).isEqualTo(ActorType.SYSTEM);
        assertThat(att.verdict).isEqualTo(AttestationVerdict.FLAGGED);
        assertThat(att.confidence).isEqualTo(1.0);
        assertThat(att.capabilityTag).isEqualTo("code-review");
        assertThat(att.trustDimension).isEqualTo("behavioral:latency");
        assertThat(att.dimensionScore).isEqualTo(0.0);
        assertThat(att.evidence).isEqualTo("28500ms exceeded 5000ms p50");
        assertThat(att.ledgerEntryId).isEqualTo(entryId);
        assertThat(att.subjectId).isEqualTo(subjectId);
    }

    @Test
    void compliance_creates_sound_attestation() {
        var entryId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();
        var att = ComplianceAttestations.compliance(entryId, subjectId,
                "code-review", "latency", 1.0);

        assertThat(att.verdict).isEqualTo(AttestationVerdict.SOUND);
        assertThat(att.trustDimension).isEqualTo("behavioral:latency");
        assertThat(att.dimensionScore).isEqualTo(1.0);
    }
}
