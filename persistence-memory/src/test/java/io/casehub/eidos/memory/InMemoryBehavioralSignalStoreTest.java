package io.casehub.eidos.memory;

import io.casehub.eidos.api.BehavioralSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static io.casehub.eidos.api.BehavioralSignal.DECLINE;
import static io.casehub.eidos.api.BehavioralSignal.SUCCESS;
import static io.casehub.eidos.api.BehavioralSignal.COMPLIANT;
import static io.casehub.eidos.api.BehavioralSignal.VIOLATED;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBehavioralSignalStoreTest {

    InMemoryBehavioralSignalStore store;

    @BeforeEach
    void setUp() throws Exception {
        store = new InMemoryBehavioralSignalStore();
        setTtl(store, "declineTtlDays", 30);
        setTtl(store, "successTtlDays", 30);
        setTtl(store, "compliantTtlDays", 30);
        setTtl(store, "violatedTtlDays", 90);
    }

    static void setTtl(final InMemoryBehavioralSignalStore s,
                        final String fieldName, final int days) throws Exception {
        final Field f = InMemoryBehavioralSignalStore.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(s, days);
    }

    @Test
    void single_decline_counted() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void multiple_declines_accumulate() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(3);
    }

    @Test
    void learned_exclusions_returns_populated_domains_with_counts() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);

        final var exclusions = store.learned("a1", "t1", "code-review", DECLINE);
        assertThat(exclusions).containsEntry("rust", 2).containsEntry("go", 1);
    }

    @Test
    void empty_map_when_no_declines() {
        assertThat(store.learned("a1", "t1", "code-review", DECLINE)).isEmpty();
    }

    @Test
    void zero_count_for_unknown_triple() {
        assertThat(store.count("unknown", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    @Test
    void zero_count_for_unknown_domain() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "go", DECLINE)).isEqualTo(0);
    }

    @Test
    void clear_declines_removes_all_domains() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);
        store.clear("a1", "t1", "code-review", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review", "go", DECLINE)).isEqualTo(0);
    }

    @Test
    void clear_code_review_does_not_remove_code_review_enhanced() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review-enhanced", "rust", DECLINE);
        store.clear("a1", "t1", "code-review", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review-enhanced", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void isolation_by_agent_id() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a2", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    @Test
    void isolation_by_tenancy_id() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t2", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    @Test
    void expired_entries_not_counted() throws Exception {
        setTtl(store, "declineTtlDays", -1);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.learned("a1", "t1", "code-review", DECLINE)).isEmpty();
    }

    @Test
    void record_decline_purges_expired_entries_before_inserting() throws Exception {
        setTtl(store, "declineTtlDays", -1);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        setTtl(store, "declineTtlDays", 30);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void concurrent_record_decline_is_thread_safe() throws Exception {
        final int threads = 20;
        final Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> store.record("a1", "t1", "code-review", "rust", DECLINE));
        }
        for (final Thread t : ts) t.start();
        for (final Thread t : ts) t.join();
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(threads);
    }

    // --- SUCCESS signal tests ---

    @Test
    void record_success_increments_count() {
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(1);
    }

    @Test
    void multiple_successes_accumulate() {
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(3);
    }

    @Test
    void success_expires_after_ttl() throws Exception {
        setTtl(store, "successTtlDays", -1);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(0);
        assertThat(store.learned("a1", "t1", "code-review", SUCCESS)).isEmpty();
    }

    @Test
    void learned_returns_success_domains() {
        store.record("a1", "t1", "code-review", "java", SUCCESS);
        store.record("a1", "t1", "code-review", "java", SUCCESS);
        store.record("a1", "t1", "code-review", "python", SUCCESS);

        final var proficiencies = store.learned("a1", "t1", "code-review", SUCCESS);
        assertThat(proficiencies).containsEntry("java", 2).containsEntry("python", 1);
    }

    @Test
    void clear_success_does_not_affect_declines() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);

        store.clear("a1", "t1", "code-review", SUCCESS);

        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(2);
    }

    @Test
    void decline_and_success_coexist_independently() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);

        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(2);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(3);

        final var declines = store.learned("a1", "t1", "code-review", DECLINE);
        final var successes = store.learned("a1", "t1", "code-review", SUCCESS);
        assertThat(declines).containsEntry("rust", 2);
        assertThat(successes).containsEntry("rust", 3);
    }

    // --- COMPLIANT signal tests ---

    @Test
    void compliant_signal_recorded_and_counted() {
        store.record("a1", "t1", "code-review", "latency", COMPLIANT);
        assertThat(store.count("a1", "t1", "code-review", "latency", COMPLIANT)).isEqualTo(1);
    }

    @Test
    void violated_signal_recorded_and_counted() {
        store.record("a1", "t1", "code-review", "latency", VIOLATED);
        assertThat(store.count("a1", "t1", "code-review", "latency", VIOLATED)).isEqualTo(1);
    }

    @Test
    void violated_expires_after_ttl() throws Exception {
        setTtl(store, "violatedTtlDays", -1);
        store.record("a1", "t1", "code-review", "latency", VIOLATED);
        assertThat(store.count("a1", "t1", "code-review", "latency", VIOLATED)).isEqualTo(0);
    }

    @Test
    void compliant_and_violated_coexist_independently() {
        store.record("a1", "t1", "code-review", "latency", COMPLIANT);
        store.record("a1", "t1", "code-review", "latency", COMPLIANT);
        store.record("a1", "t1", "code-review", "latency", VIOLATED);

        assertThat(store.count("a1", "t1", "code-review", "latency", COMPLIANT)).isEqualTo(2);
        assertThat(store.count("a1", "t1", "code-review", "latency", VIOLATED)).isEqualTo(1);
    }

    @Test
    void learned_returns_violated_qualifiers() {
        store.record("a1", "t1", "code-review", "latency", VIOLATED);
        store.record("a1", "t1", "code-review", "latency", VIOLATED);
        store.record("a1", "t1", "code-review", "attestation-rate", VIOLATED);

        var violations = store.learned("a1", "t1", "code-review", VIOLATED);
        assertThat(violations).containsEntry("latency", 2).containsEntry("attestation-rate", 1);
    }
}
