package io.casehub.eidos.org.runtime.trust;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.OrgAwareDispatch;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.ledger.api.model.LedgerEntry;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgDistanceAttestationEnricherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OrgAwareDispatch orgAwareDispatch;
    private OrgDistanceAttestationEnricher enricher;

    @BeforeEach
    void setUp() {
        orgAwareDispatch = mock(OrgAwareDispatch.class);
        enricher = new OrgDistanceAttestationEnricher(orgAwareDispatch);
    }

    private LedgerAttestation attestation(String attestorId) {
        var a = new LedgerAttestation();
        a.id = UUID.randomUUID();
        a.attestorId = attestorId;
        a.attestorType = ActorType.AGENT;
        a.relevanceSnapshot = null;
        return a;
    }

    private LedgerEntry entry(String actorId, String tenancyId) {
        return new TestLedgerEntry(actorId, tenancyId);
    }

    @Test
    void name_returns_org_distance() {
        assertThat(enricher.name()).isEqualTo("org-distance");
    }

    @Test
    void writes_org_distance_to_snapshot() throws Exception {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(2);

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        assertThat(att.relevanceSnapshot).isNotNull();
        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 2);
    }

    @Test
    void same_unit_distance_1() throws Exception {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(1);

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 1);
    }

    @Test
    void unknown_distance_leaves_snapshot_null() {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(-1);

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        assertThat(att.relevanceSnapshot).isNull();
    }

    @Test
    void zero_distance_self_still_written() throws Exception {
        when(orgAwareDispatch.orgDistance("agent-1", "agent-1", "tenant-1"))
                .thenReturn(0);

        var att = attestation("agent-1");
        enricher.enrich(att, entry("agent-1", "tenant-1"));

        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 0);
    }

    @Test
    void merges_into_existing_snapshot() throws Exception {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(3);

        var att = attestation("attestor-1");
        att.relevanceSnapshot = "{\"other-factor\": 42}";
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 3);
        assertThat(snapshot).containsEntry("other-factor", 42);
    }

    @Test
    void large_distance_written_correctly() throws Exception {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(7);

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 7);
    }

    @Test
    void round_trip_enricher_to_factor() throws Exception {
        when(orgAwareDispatch.orgDistance("attestor-1", "actor-1", "tenant-1"))
                .thenReturn(2);

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("actor-1", "tenant-1"));

        // Parse the snapshot the same way RelevanceComposer builds the context
        Map<String, Object> snapshot = MAPPER.readValue(
                att.relevanceSnapshot, new TypeReference<>() {});

        var factor = new OrgProximityRelevanceFactor(java.util.List.of(1.0, 0.7, 0.65, 0.6));
        var ctx = new io.casehub.ledger.api.spi.AttestationRelevanceContext(
                att.id, att.attestorId, att.attestorType, att.attestorRole,
                "actor-1", "tenant-1", "cap", null,
                java.time.Instant.now(), snapshot);
        var result = factor.assess(ctx);
        assertThat(result.weight()).isEqualTo(0.7);
        assertThat(result.reason()).contains("org-distance:2");
    }

    static class TestLedgerEntry extends LedgerEntry {
        TestLedgerEntry(String actorId, String tenancyId) {
            this.actorId = actorId;
            this.tenancyId = tenancyId;
        }
    }
}
