package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.DetectedRoleSignal;
import io.casehub.eidos.api.DetectedRoleSignalStore;
import io.casehub.eidos.api.RoleEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StigmergyEvidenceSourceTest {

    private StubSignalStore store;
    private StigmergyEvidenceSource source;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        store = new StubSignalStore();
        source = new StigmergyEvidenceSource(store, 90, 90.0, 0.5);
    }

    @Test
    void sourceId_is_stigmergy() {
        assertThat(source.sourceId()).isEqualTo("stigmergy");
    }

    @Test
    void empty_store_returns_zero_evidence() {
        RoleEvidence evidence = source.evaluate("a1", "coordinator", "t1",
            ProbeContext.of(null));
        assertThat(evidence.qualitySum()).isZero();
        assertThat(evidence.effectiveCount()).isZero();
        assertThat(evidence.weight()).isEqualTo(0.5);
    }

    @Test
    void single_recent_signal_full_weight() {
        store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));

        RoleEvidence evidence = source.evaluate("a1", "coordinator", "t1",
            ProbeContext.of(null));
        assertThat(evidence.effectiveCount()).isCloseTo(1.0, within(0.05));
        assertThat(evidence.qualitySum()).isCloseTo(0.9, within(0.05));
    }

    @Test
    void old_signal_decayed() {
        Instant old = NOW.minus(45, ChronoUnit.DAYS);
        store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 1.0, old));

        RoleEvidence evidence = source.evaluate("a1", "coordinator", "t1",
            ProbeContext.of(null));
        assertThat(evidence.effectiveCount()).isCloseTo(0.707, within(0.05));
        assertThat(evidence.qualitySum()).isCloseTo(0.707, within(0.05));
    }

    @Test
    void different_role_not_included() {
        store.add(new DetectedRoleSignal("a1", "t1", "reviewer",
            "review-initiation", null, 0.9, NOW));

        RoleEvidence evidence = source.evaluate("a1", "coordinator", "t1",
            ProbeContext.of(null));
        assertThat(evidence.effectiveCount()).isZero();
    }

    @Test
    void weight_configurable() {
        var customSource = new StigmergyEvidenceSource(store, 90, 90.0, 0.3);
        store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));
        RoleEvidence evidence = customSource.evaluate("a1", "coordinator", "t1",
            ProbeContext.of(null));
        assertThat(evidence.weight()).isEqualTo(0.3);
    }

    static class StubSignalStore implements DetectedRoleSignalStore {
        private final List<DetectedRoleSignal> signals = new ArrayList<>();

        void add(DetectedRoleSignal signal) { signals.add(signal); }

        @Override public void record(DetectedRoleSignal signal) { signals.add(signal); }

        @Override
        public List<DetectedRoleSignal> signals(String agentId, String roleName,
                                                 String tenancyId, Instant since) {
            return signals.stream()
                .filter(s -> s.agentId().equals(agentId))
                .filter(s -> s.roleName().equals(roleName))
                .filter(s -> s.tenancyId().equals(tenancyId))
                .filter(s -> !s.observedAt().isBefore(since))
                .toList();
        }

        @Override
        public List<DetectedRoleSignal> signalsByAgent(String agentId,
                                                        String tenancyId, Instant since) {
            return signals.stream()
                .filter(s -> s.agentId().equals(agentId))
                .filter(s -> s.tenancyId().equals(tenancyId))
                .filter(s -> !s.observedAt().isBefore(since))
                .toList();
        }

        @Override public int purge(String agentId, String tenancyId, Instant olderThan) { return 0; }
        @Override public void clear(String agentId, String tenancyId) { signals.clear(); }
    }
}
