package io.casehub.eidos.memory;

import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.BehavioralSignalStoreContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static io.casehub.eidos.api.BehavioralSignal.COMPLIANT;
import static io.casehub.eidos.api.BehavioralSignal.DECLINE;
import static io.casehub.eidos.api.BehavioralSignal.SUCCESS;
import static io.casehub.eidos.api.BehavioralSignal.VIOLATED;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBehavioralSignalStoreTest extends BehavioralSignalStoreContractTest {

    InMemoryBehavioralSignalStore store;

    @Override
    protected BehavioralSignalStore store() {
        return store;
    }

    @BeforeEach
    @Override
    protected void resetStore() {
        try {
            store = new InMemoryBehavioralSignalStore();
            setTtl(store, "declineTtlDays", 30);
            setTtl(store, "successTtlDays", 30);
            setTtl(store, "compliantTtlDays", 30);
            setTtl(store, "violatedTtlDays", 90);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setTtl(final InMemoryBehavioralSignalStore s,
                        final String fieldName, final int days) throws Exception {
        final Field f = InMemoryBehavioralSignalStore.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(s, days);
    }

    // --- In-memory-specific: TTL expiry ---

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
    void success_expires_after_ttl() throws Exception {
        setTtl(store, "successTtlDays", -1);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(0);
        assertThat(store.learned("a1", "t1", "code-review", SUCCESS)).isEmpty();
    }

    @Test
    void compliant_expires_after_ttl() throws Exception {
        setTtl(store, "compliantTtlDays", -1);
        store.record("a1", "t1", "code-review", "latency", COMPLIANT);
        assertThat(store.count("a1", "t1", "code-review", "latency", COMPLIANT)).isEqualTo(0);
        assertThat(store.learned("a1", "t1", "code-review", COMPLIANT)).isEmpty();
    }

    @Test
    void violated_expires_after_ttl() throws Exception {
        setTtl(store, "violatedTtlDays", -1);
        store.record("a1", "t1", "code-review", "latency", VIOLATED);
        assertThat(store.count("a1", "t1", "code-review", "latency", VIOLATED)).isEqualTo(0);
        assertThat(store.learned("a1", "t1", "code-review", VIOLATED)).isEmpty();
    }

    // --- In-memory-specific: thread safety ---

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
}
