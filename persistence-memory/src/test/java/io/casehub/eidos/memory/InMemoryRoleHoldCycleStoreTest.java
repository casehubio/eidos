package io.casehub.eidos.memory;

import io.casehub.eidos.api.RoleHoldCycleStore.HoldCycleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRoleHoldCycleStoreTest {

    private InMemoryRoleHoldCycleStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRoleHoldCycleStore();
    }

    @Test
    void get_returns_empty_for_unknown_key() {
        var state = store.get("a1", "coordinator", "t1");
        assertThat(state).isEqualTo(HoldCycleState.EMPTY);
    }

    @Test
    void increment_creates_and_increments() {
        store.increment("a1", "coordinator", "t1");
        var state = store.get("a1", "coordinator", "t1");
        assertThat(state.consecutiveCycles()).isEqualTo(1);
        assertThat(state.lastEvaluatedAt()).isNotNull();
    }

    @Test
    void increment_accumulates() {
        store.increment("a1", "coordinator", "t1");
        store.increment("a1", "coordinator", "t1");
        store.increment("a1", "coordinator", "t1");
        assertThat(store.get("a1", "coordinator", "t1").consecutiveCycles()).isEqualTo(3);
    }

    @Test
    void reset_clears_cycles_and_updates_timestamp() {
        store.increment("a1", "coordinator", "t1");
        store.increment("a1", "coordinator", "t1");
        store.reset("a1", "coordinator", "t1");
        var state = store.get("a1", "coordinator", "t1");
        assertThat(state.consecutiveCycles()).isEqualTo(0);
        assertThat(state.lastEvaluatedAt()).isNotNull();
    }

    @Test
    void different_roles_independent() {
        store.increment("a1", "coordinator", "t1");
        store.increment("a1", "analyst", "t1");
        store.increment("a1", "analyst", "t1");
        assertThat(store.get("a1", "coordinator", "t1").consecutiveCycles()).isEqualTo(1);
        assertThat(store.get("a1", "analyst", "t1").consecutiveCycles()).isEqualTo(2);
    }

    @Test
    void concurrent_increment_is_thread_safe() throws Exception {
        final int threads = 20;
        final Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> store.increment("a1", "coordinator", "t1"));
        }
        for (final Thread t : ts) t.start();
        for (final Thread t : ts) t.join();
        assertThat(store.get("a1", "coordinator", "t1").consecutiveCycles()).isEqualTo(threads);
    }
}
