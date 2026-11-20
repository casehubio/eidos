package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.BehavioralSignalStoreContractTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

@QuarkusTest
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresJpaBehavioralSignalStoreTest extends BehavioralSignalStoreContractTest {

    @Inject BehavioralSignalStore store;
    @Inject EntityManager em;

    @Override
    protected BehavioralSignalStore store() {
        return store;
    }

    @BeforeEach
    @Override
    @Transactional
    protected void resetStore() {
        em.createNativeQuery("DELETE FROM behavioral_signal").executeUpdate();
    }
}
