package io.casehub.eidos.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCapabilitySpecializationStoreTest {

    InMemoryCapabilitySpecializationStore store;

    @BeforeEach
    void setUp() throws Exception {
        store = new InMemoryCapabilitySpecializationStore();
        setTtl(store, 30);
    }

    static void setTtl(InMemoryCapabilitySpecializationStore s, int days) throws Exception {
        Field f = InMemoryCapabilitySpecializationStore.class.getDeclaredField("declineTtlDays");
        f.setAccessible(true);
        f.setInt(s, days);
    }

    @Test
    void single_decline_counted() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(1);
    }

    @Test
    void multiple_declines_accumulate() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(3);
    }

    @Test
    void learned_exclusions_returns_populated_domains_with_counts() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "go");

        var exclusions = store.learnedExclusions("a1", "t1", "code-review");
        assertThat(exclusions).containsEntry("rust", 2).containsEntry("go", 1);
    }

    @Test
    void empty_map_when_no_declines() {
        assertThat(store.learnedExclusions("a1", "t1", "code-review")).isEmpty();
    }

    @Test
    void zero_count_for_unknown_triple() {
        assertThat(store.declineCount("unknown", "t1", "code-review", "rust")).isEqualTo(0);
    }

    @Test
    void zero_count_for_unknown_domain() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "go")).isEqualTo(0);
    }

    @Test
    void clear_declines_removes_all_domains() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "go");
        store.clearDeclines("a1", "t1", "code-review");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(0);
        assertThat(store.declineCount("a1", "t1", "code-review", "go")).isEqualTo(0);
    }

    @Test
    void clear_code_review_does_not_remove_code_review_enhanced() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review-enhanced", "rust");
        store.clearDeclines("a1", "t1", "code-review");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(0);
        assertThat(store.declineCount("a1", "t1", "code-review-enhanced", "rust")).isEqualTo(1);
    }

    @Test
    void isolation_by_agent_id() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a2", "t1", "code-review", "rust")).isEqualTo(0);
    }

    @Test
    void isolation_by_tenancy_id() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t2", "code-review", "rust")).isEqualTo(0);
    }

    @Test
    void expired_entries_not_counted() throws Exception {
        setTtl(store, -1);   // TTL = -1 day → expires 1 day in the past → immediately stale
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(0);
        assertThat(store.learnedExclusions("a1", "t1", "code-review")).isEmpty();
    }

    @Test
    void record_decline_purges_expired_entries_before_inserting() throws Exception {
        setTtl(store, -1);
        store.recordDecline("a1", "t1", "code-review", "rust");  // expires immediately
        store.recordDecline("a1", "t1", "code-review", "rust");  // expires immediately
        setTtl(store, 30);
        store.recordDecline("a1", "t1", "code-review", "rust");  // purges the two expired, adds one fresh
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(1);
    }

    @Test
    void concurrent_record_decline_is_thread_safe() throws Exception {
        final int threads = 20;
        final Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> store.recordDecline("a1", "t1", "code-review", "rust"));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(threads);
    }
}
