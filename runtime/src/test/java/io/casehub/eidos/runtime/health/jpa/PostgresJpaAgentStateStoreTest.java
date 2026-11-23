package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.AgentStateStoreContractTest;
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
class PostgresJpaAgentStateStoreTest extends AgentStateStoreContractTest {

    @Inject AgentStateStore store;
    @Inject EntityManager em;

    @Override
    protected AgentStateStore store() {
        return store;
    }

    @BeforeEach
    @Override
    @Transactional
    protected void resetStore() {
        em.createNativeQuery("DELETE FROM agent_state").executeUpdate();
    }
}
