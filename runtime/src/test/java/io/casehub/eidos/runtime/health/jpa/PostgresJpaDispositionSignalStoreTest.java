package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionSignalStoreContractTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Tag;

@QuarkusTest
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresJpaDispositionSignalStoreTest extends DispositionSignalStoreContractTest {

    @Inject DispositionSignalStore store;
    @Inject EntityManager em;

    @Override
    protected DispositionSignalStore store() {
        return store;
    }

    @BeforeEach
    @Override
    @Transactional
    protected void resetStore() {
        em.createNativeQuery("DELETE FROM disposition_signal").executeUpdate();
    }
}
