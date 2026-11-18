package io.casehub.eidos.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InMemoryDispositionSignalStoreTest {

    InMemoryDispositionSignalStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryDispositionSignalStore();
    }

    @Test
    void recordActivation_increments_count() {
        store.recordActivation("agent-1", "tenant-1", "ti");
        store.recordActivation("agent-1", "tenant-1", "ti");
        var counts = store.activationWeights("agent-1", "tenant-1");
        assertThat(counts).containsEntry("ti", 2.0);
    }

    @Test
    void recordActivation_multiple_functions() {
        store.recordActivation("a", "t", "ti");
        store.recordActivation("a", "t", "ne");
        store.recordActivation("a", "t", "ne");
        var counts = store.activationWeights("a", "t");
        assertThat(counts).containsEntry("ti", 1.0).containsEntry("ne", 2.0);
    }

    @Test
    void activationWeights_empty_when_no_signals() {
        assertThat(store.activationWeights("a", "t")).isEmpty();
    }

    @Test
    void decay_retains_fraction_of_counts() {
        for (int i = 0; i < 10; i++) {store.recordActivation("a", "t", "ti");}
        store.decay("a", "t", 0.20);
        // 0.20 retention: 10 * 0.2 = 2.0
        assertThat(store.activationWeights("a", "t")).containsEntry("ti", 2.0);
    }

    @Test
    void decay_single_activation_retains_fractional_weight() {
        store.recordActivation("a", "t", "ne");
        store.decay("a", "t", 0.20);
        // 1.0 * 0.2 = 0.2 — no longer truncated to zero
        assertThat(store.activationWeights("a", "t")).containsEntry("ne", 0.2);
    }

    @Test
    void decay_retains_fraction_of_higher_counts() {
        for (int i = 0; i < 100; i++) {store.recordActivation("a", "t", "fi");}
        store.decay("a", "t", 0.20);
        // 0.20 retention: 100 * 0.2 = 20.0
        assertThat(store.activationWeights("a", "t")).containsEntry("fi", 20.0);
    }

    @Test
    void decay_preserves_fractional_weight() {
        for (int i = 0; i < 3; i++) {store.recordActivation("a", "t", "se");}
        store.decay("a", "t", 0.70);
        // 3.0 * 0.7 = 2.1 — no longer truncated
        assertThat(store.activationWeights("a", "t").get("se")).isCloseTo(2.1, within(0.001));
    }

    @Test
    void decay_zero_factor_clears_all() {
        for (int i = 0; i < 5; i++) {store.recordActivation("a", "t", "ti");}
        store.decay("a", "t", 0.0);
        assertThat(store.activationWeights("a", "t")).doesNotContainKey("ti");
    }

    @Test
    void decay_full_factor_retains_all() {
        for (int i = 0; i < 5; i++) {store.recordActivation("a", "t", "ti");}
        store.decay("a", "t", 1.0);
        assertThat(store.activationWeights("a", "t")).containsEntry("ti", 5.0);
    }

    @Test
    void decay_removes_zero_weight_entries() {
        store.recordActivation("a", "t", "ne");
        store.decay("a", "t", 0.0);
        // 0.0 retention: 1.0 * 0.0 = 0.0 → removed
        assertThat(store.activationWeights("a", "t")).doesNotContainKey("ne");
    }

    @Test
    void clear_removes_all() {
        store.recordActivation("a", "t", "ti");
        store.recordActivation("a", "t", "ne");
        store.clear("a", "t");
        assertThat(store.activationWeights("a", "t")).isEmpty();
    }

    @Test
    void tenancy_isolation() {
        store.recordActivation("a", "t1", "ti");
        store.recordActivation("a", "t2", "fe");
        assertThat(store.activationWeights("a", "t1")).containsOnlyKeys("ti");
        assertThat(store.activationWeights("a", "t2")).containsOnlyKeys("fe");
    }

    @Test
    void agent_isolation() {
        store.recordActivation("a1", "t", "ti");
        store.recordActivation("a2", "t", "fe");
        assertThat(store.activationWeights("a1", "t")).containsOnlyKeys("ti");
        assertThat(store.activationWeights("a2", "t")).containsOnlyKeys("fe");
    }

    @Test
    void clear_does_not_affect_other_agents() {
        store.recordActivation("a1", "t", "ti");
        store.recordActivation("a2", "t", "fe");
        store.clear("a1", "t");
        assertThat(store.activationWeights("a1", "t")).isEmpty();
        assertThat(store.activationWeights("a2", "t")).containsEntry("fe", 1.0);
    }

    @Test
    void decay_does_not_affect_other_agents() {
        for (int i = 0; i < 10; i++) {store.recordActivation("a1", "t", "ti");}
        for (int i = 0; i < 10; i++) {store.recordActivation("a2", "t", "ti");}
        store.decay("a1", "t", 0.50);
        assertThat(store.activationWeights("a1", "t")).containsEntry("ti", 5.0);
        assertThat(store.activationWeights("a2", "t")).containsEntry("ti", 10.0);
    }

    @Test
    void concurrent_recordActivation_is_thread_safe() throws Exception {
        final int      threads = 20;
        final Thread[] ts      = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> store.recordActivation("a", "t", "ti"));
        }
        for (final Thread t : ts) {t.start();}
        for (final Thread t : ts) {t.join();}
        assertThat(store.activationWeights("a", "t")).containsEntry("ti", (double) threads);
    }

    @Test
    void activationWeights_returns_immutable_map() {
        store.recordActivation("a", "t", "ti");
        var counts = store.activationWeights("a", "t");
        assertThat(counts).isUnmodifiable();
    }

    @Test
    void concurrent_n_threads_m_activations_exact_count() throws Exception {
        final int      threads   = 10;
        final int      perThread = 50;
        final Thread[] ts        = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    store.recordActivation("a", "t", "fi");
                }
            });
        }
        for (final Thread t : ts) {t.start();}
        for (final Thread t : ts) {t.join();}
        assertThat(store.activationWeights("a", "t")).containsEntry("fi", (double) (threads * perThread));
    }

    @Test
    void concurrent_decay_and_recordActivation_loses_no_increment() throws Exception {
        for (int i = 0; i < 100; i++) {store.recordActivation("a", "t", "ti");}
        final Thread decayer = new Thread(() -> store.decay("a", "t", 0.5));
        final Thread adder   = new Thread(() -> store.recordActivation("a", "t", "ti"));
        decayer.start();
        adder.start();
        decayer.join();
        adder.join();
        double weight = store.activationWeights("a", "t").getOrDefault("ti", 0.0);
        // Either decay then add (50.0 + 1.0 = 51.0) or add then decay ((101) * 0.5 = 50.5)
        assertThat(weight).isIn(51.0, 50.5);
    }

    @Test
    void concurrent_first_activations_do_not_throw() throws Exception {
        final int      threads = 20;
        final Thread[] ts      = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final String term = "f" + i;
            ts[i] = new Thread(() -> store.recordActivation("a", "t", term));
        }
        for (final Thread t : ts) {t.start();}
        for (final Thread t : ts) {t.join();}
        assertThat(store.activationWeights("a", "t")).hasSize(threads);
    }

}
