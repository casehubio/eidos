package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.AgentStateStoreContractTest;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;

@QuarkusTest
@TestTransaction
@TestProfile(PostgresTestProfile.class)
@Tag("testcontainers")
class PostgresJpaAgentStateStoreTest extends AgentStateStoreContractTest {

    @Inject AgentStateStore store;

    @Override
    protected AgentStateStore store() {
        return store;
    }
}
