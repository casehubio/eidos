package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.AgentStateStoreContractTest;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@TestTransaction
class JpaAgentStateStoreTest extends AgentStateStoreContractTest {

    @Inject
    AgentStateStore store;

    @Override
    protected AgentStateStore store() {
        return store;
    }
}
