package io.casehub.eidos.memory;

import io.casehub.eidos.api.DetectedRoleSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDetectedRoleSignalStoreTest {

    private InMemoryDetectedRoleSignalStore store;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        store = new InMemoryDetectedRoleSignalStore();
    }

    @Test
    void record_and_query_by_role() {
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));
        store.record(new DetectedRoleSignal("a1", "t1", "reviewer",
            "review-initiation", null, 0.8, NOW));

        var signals = store.signals("a1", "coordinator", "t1",
            NOW.minus(1, ChronoUnit.DAYS));
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).roleName()).isEqualTo("coordinator");
    }

    @Test
    void signals_by_agent_returns_all_roles() {
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));
        store.record(new DetectedRoleSignal("a1", "t1", "reviewer",
            "review-initiation", null, 0.8, NOW));

        var signals = store.signalsByAgent("a1", "t1",
            NOW.minus(1, ChronoUnit.DAYS));
        assertThat(signals).hasSize(2);
    }

    @Test
    void since_filter_excludes_old_signals() {
        final Instant old = NOW.minus(100, ChronoUnit.DAYS);
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, old));
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.8, NOW));

        var signals = store.signals("a1", "coordinator", "t1",
            NOW.minus(1, ChronoUnit.DAYS));
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).confidence()).isEqualTo(0.8);
    }

    @Test
    void purge_removes_old_signals() {
        final Instant old = NOW.minus(200, ChronoUnit.DAYS);
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, old));
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.8, NOW));

        final int purged = store.purge("a1", "t1", NOW.minus(1, ChronoUnit.DAYS));
        assertThat(purged).isEqualTo(1);
        assertThat(store.signalsByAgent("a1", "t1", Instant.EPOCH)).hasSize(1);
    }

    @Test
    void clear_removes_all() {
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));
        store.clear("a1", "t1");
        assertThat(store.signalsByAgent("a1", "t1", Instant.EPOCH)).isEmpty();
    }

    @Test
    void different_tenancies_isolated() {
        store.record(new DetectedRoleSignal("a1", "t1", "coordinator",
            "delegation", null, 0.9, NOW));
        store.record(new DetectedRoleSignal("a1", "t2", "coordinator",
            "delegation", null, 0.8, NOW));

        assertThat(store.signals("a1", "coordinator", "t1",
            NOW.minus(1, ChronoUnit.DAYS))).hasSize(1);
        assertThat(store.signals("a1", "coordinator", "t2",
            NOW.minus(1, ChronoUnit.DAYS))).hasSize(1);
    }
}
