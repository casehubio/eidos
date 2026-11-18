package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DispositionSignalStore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresJpaDispositionSignalStoreTest {

    @Inject DispositionSignalStore store;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void cleanTable() {
        em.createNativeQuery("DELETE FROM disposition_signal").executeUpdate();
    }

    @Test
    void upsert_creates_new_entry() {
        store.recordActivation("a1", "t1", "Ti");
        assertThat(store.activationWeights("a1", "t1"))
                .containsEntry("Ti", 1.0);
    }

    @Test
    void upsert_increments_existing_entry() {
        store.recordActivation("a1", "t1", "Ti");
        store.recordActivation("a1", "t1", "Ti");
        store.recordActivation("a1", "t1", "Ti");
        assertThat(store.activationWeights("a1", "t1"))
                .containsEntry("Ti", 3.0);
    }

    @Test
    void multiple_terms_coexist() {
        store.recordActivation("a1", "t1", "Ti");
        store.recordActivation("a1", "t1", "Fe");
        store.recordActivation("a1", "t1", "Fe");
        var weights = store.activationWeights("a1", "t1");
        assertThat(weights).containsEntry("Ti", 1.0).containsEntry("Fe", 2.0);
    }

    @Test
    void decay_reduces_weights() {
        store.recordActivation("a1", "t1", "Ti");
        store.recordActivation("a1", "t1", "Ti");
        store.decay("a1", "t1", 0.5);
        assertThat(store.activationWeights("a1", "t1"))
                .containsEntry("Ti", 1.0);
    }

    @Test
    void clear_removes_all_entries() {
        store.recordActivation("a1", "t1", "Ti");
        store.recordActivation("a1", "t1", "Fe");
        store.clear("a1", "t1");
        assertThat(store.activationWeights("a1", "t1")).isEmpty();
    }

    @Test
    void isolation_by_tenancy() {
        store.recordActivation("a1", "t1", "Ti");
        assertThat(store.activationWeights("a1", "t2")).isEmpty();
    }
}
