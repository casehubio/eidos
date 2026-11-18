package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.BehavioralSignalStore;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.casehub.eidos.api.BehavioralSignal.*;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestTransaction
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresJpaBehavioralSignalStoreTest {

    @Inject BehavioralSignalStore store;

    @Test
    void upsert_increments_on_conflict() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(3);
    }

    @Test
    void upsert_across_signal_types() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", COMPLIANT);
        store.record("a1", "t1", "code-review", "rust", VIOLATED);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(1);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(1);
        assertThat(store.count("a1", "t1", "code-review", "rust", COMPLIANT)).isEqualTo(1);
        assertThat(store.count("a1", "t1", "code-review", "rust", VIOLATED)).isEqualTo(1);
    }

    @Test
    void learned_returns_populated_domains() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);
        var exclusions = store.learned("a1", "t1", "code-review", DECLINE);
        assertThat(exclusions).containsEntry("rust", 2).containsEntry("go", 1);
    }

    @Test
    void clear_removes_entries() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);
        store.clear("a1", "t1", "code-review", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review", "go", DECLINE)).isEqualTo(0);
    }

    @Test
    void isolation_by_tenancy() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t2", "code-review", "rust", DECLINE)).isEqualTo(0);
    }
}
