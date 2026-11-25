package io.casehub.eidos.runtime.trust;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.*;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.ledger.api.model.LedgerEntry;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilityOverlapAttestationEnricherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentRegistry registry;
    private CapabilityOverlapAttestationEnricher enricher;

    @BeforeEach
    void setUp() {
        registry = mock(AgentRegistry.class);
        enricher = new CapabilityOverlapAttestationEnricher(registry);
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

    private AgentDescriptor agent(String id, String tenancyId, String... capabilities) {
        var caps = java.util.Arrays.stream(capabilities)
                .map(c -> AgentCapability.builder().name(c).build())
                .toList();
        return AgentDescriptor.builder()
                .agentId(id).name(id).tenancyId(tenancyId)
                .slot("worker")
                .capabilities(caps)
                .disposition(AgentDisposition.builder().build())
                .build();
    }

    @Test
    void name_returns_capability_overlap() {
        assertThat(enricher.name()).isEqualTo("capability-overlap");
    }

    @Test
    void full_overlap_writes_one() throws Exception {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1", "code-review", "testing")));
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1", "code-review", "testing")));

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("scored-1", "t1"));

        Map<String, Object> snapshot = MAPPER.readValue(att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(((Number) snapshot.get("capability-overlap")).doubleValue()).isEqualTo(1.0);
    }

    @Test
    void no_overlap_writes_zero() throws Exception {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1", "code-review")));
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1", "testing")));

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("scored-1", "t1"));

        Map<String, Object> snapshot = MAPPER.readValue(att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(((Number) snapshot.get("capability-overlap")).doubleValue()).isEqualTo(0.0);
    }

    @Test
    void partial_overlap_writes_jaccard() throws Exception {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1", "code-review", "testing", "planning")));
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1", "code-review", "deployment")));

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("scored-1", "t1"));

        Map<String, Object> snapshot = MAPPER.readValue(att.relevanceSnapshot, new TypeReference<>() {});
        double overlap = ((Number) snapshot.get("capability-overlap")).doubleValue();
        assertThat(overlap).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void unknown_attestor_skips_enrichment() {
        when(registry.findById("unknown", "t1")).thenReturn(java.util.Optional.empty());
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1", "code-review")));

        var att = attestation("unknown");
        enricher.enrich(att, entry("scored-1", "t1"));

        assertThat(att.relevanceSnapshot).isNull();
    }

    @Test
    void unknown_scored_agent_skips_enrichment() {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1", "code-review")));
        when(registry.findById("scored-1", "t1")).thenReturn(java.util.Optional.empty());

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("scored-1", "t1"));

        assertThat(att.relevanceSnapshot).isNull();
    }

    @Test
    void merges_into_existing_snapshot() throws Exception {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1", "code-review")));
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1", "code-review")));

        var att = attestation("attestor-1");
        att.relevanceSnapshot = "{\"org-distance\": 2}";
        enricher.enrich(att, entry("scored-1", "t1"));

        Map<String, Object> snapshot = MAPPER.readValue(att.relevanceSnapshot, new TypeReference<>() {});
        assertThat(snapshot).containsEntry("org-distance", 2);
        assertThat(((Number) snapshot.get("capability-overlap")).doubleValue()).isEqualTo(1.0);
    }

    @Test
    void empty_capabilities_on_both_writes_zero() throws Exception {
        when(registry.findById("attestor-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("attestor-1", "t1")));
        when(registry.findById("scored-1", "t1"))
                .thenReturn(java.util.Optional.of(agent("scored-1", "t1")));

        var att = attestation("attestor-1");
        enricher.enrich(att, entry("scored-1", "t1"));

        assertThat(att.relevanceSnapshot).isNull();
    }

    static class TestLedgerEntry extends LedgerEntry {
        TestLedgerEntry(String actorId, String tenancyId) {
            this.actorId = actorId;
            this.tenancyId = tenancyId;
        }
    }
}
