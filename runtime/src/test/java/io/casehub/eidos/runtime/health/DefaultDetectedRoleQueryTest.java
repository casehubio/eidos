package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.DetectedRole;
import io.casehub.eidos.api.DetectedRoleSignal;
import io.casehub.eidos.api.DetectedRoleSignalStore;
import jakarta.enterprise.inject.Instance;
import io.casehub.platform.api.preferences.PreferenceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDetectedRoleQueryTest {

    private StubStore store;
    private DefaultDetectedRoleQuery query;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        store = new StubStore();
        query = new DefaultDetectedRoleQuery(store, 90, 90.0, emptyProviderInstance());
    }

    @Test
    void no_signals_returns_empty() {
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).isEmpty();
    }

    @Test
    void signals_below_min_count_filtered_out() {
        for (int i = 0; i < 4; i++) {
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.9, NOW.minus(i, ChronoUnit.HOURS)));
        }
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).isEmpty();
    }

    @Test
    void signals_above_threshold_returned() {
        for (int i = 0; i < 10; i++) {
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.9, NOW.minus(i, ChronoUnit.HOURS)));
        }
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).roleName()).isEqualTo("coordinator");
        assertThat(roles.get(0).signalCount()).isEqualTo(10);
    }

    @Test
    void multiple_roles_sorted_by_confidence_descending() {
        for (int i = 0; i < 10; i++) {
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.7, NOW.minus(i, ChronoUnit.HOURS)));
            store.add(new DetectedRoleSignal("a1", "t1", "reviewer",
                "review-initiation", null, 0.9, NOW.minus(i, ChronoUnit.HOURS)));
        }
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).roleName()).isEqualTo("reviewer");
    }

    @Test
    void distinct_pattern_types_collected() {
        for (int i = 0; i < 5; i++) {
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.9, NOW.minus(i, ChronoUnit.HOURS)));
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "mediation", null, 0.8, NOW.minus(i, ChronoUnit.HOURS)));
        }
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).patternTypes()).containsExactlyInAnyOrder("delegation", "mediation");
    }

    @Test
    void low_confidence_signals_filtered_by_threshold() {
        for (int i = 0; i < 10; i++) {
            store.add(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.3, NOW.minus(i, ChronoUnit.HOURS)));
        }
        List<DetectedRole> roles = query.detectedRoles("a1", "t1");
        assertThat(roles).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Instance<PreferenceProvider> emptyProviderInstance() {
        return new Instance<>() {
            @Override public Instance<PreferenceProvider> select(Annotation... qualifiers) { return this; }
            @Override public <U extends PreferenceProvider> Instance<U> select(Class<U> subtype, Annotation... qualifiers) { return (Instance<U>) this; }
            @Override public <U extends PreferenceProvider> Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, Annotation... qualifiers) { return (Instance<U>) this; }
            @Override public boolean isUnsatisfied() { return true; }
            @Override public boolean isAmbiguous() { return false; }
            @Override public boolean isResolvable() { return false; }
            @Override public PreferenceProvider get() { throw new UnsupportedOperationException(); }
            @Override public void destroy(PreferenceProvider instance) {}
            @Override public Handle<PreferenceProvider> getHandle() { throw new UnsupportedOperationException(); }
            @Override public Iterable<? extends Handle<PreferenceProvider>> handles() { return List.of(); }
            @Override public Iterator<PreferenceProvider> iterator() { return List.<PreferenceProvider>of().iterator(); }
        };
    }

    static class StubStore implements DetectedRoleSignalStore {
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
