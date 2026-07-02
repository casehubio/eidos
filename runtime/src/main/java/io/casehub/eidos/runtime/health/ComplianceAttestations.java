package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.ComplianceDimension;
import io.casehub.ledger.api.model.AttestationVerdict;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.platform.api.identity.ActorType;

import java.time.Instant;
import java.util.UUID;

public final class ComplianceAttestations {

    private ComplianceAttestations() {}

    public static LedgerAttestation violation(
            final UUID ledgerEntryId, final UUID subjectId,
            final String capabilityTag, final String dimension,
            final String evidence, final double dimensionScore) {
        return build(ledgerEntryId, subjectId, capabilityTag, dimension,
                AttestationVerdict.FLAGGED, evidence, dimensionScore);
    }

    public static LedgerAttestation compliance(
            final UUID ledgerEntryId, final UUID subjectId,
            final String capabilityTag, final String dimension,
            final double dimensionScore) {
        return build(ledgerEntryId, subjectId, capabilityTag, dimension,
                AttestationVerdict.SOUND, null, dimensionScore);
    }

    private static LedgerAttestation build(
            final UUID ledgerEntryId, final UUID subjectId,
            final String capabilityTag, final String dimension,
            final AttestationVerdict verdict, final String evidence,
            final double dimensionScore) {
        final var att = new LedgerAttestation();
        att.id = UUID.randomUUID();
        att.ledgerEntryId = ledgerEntryId;
        att.subjectId = subjectId;
        att.attestorId = ComplianceDimension.ATTESTOR_ID;
        att.attestorType = ActorType.SYSTEM;
        att.verdict = verdict;
        att.evidence = evidence;
        att.confidence = 1.0;
        att.capabilityTag = capabilityTag;
        att.trustDimension = ComplianceDimension.TRUST_DIMENSION_PREFIX + dimension;
        att.dimensionScore = dimensionScore;
        att.occurredAt = Instant.now();
        return att;
    }
}
