package io.casehub.eidos.memory;

import io.casehub.eidos.api.ReactiveAgentStateStore;
import io.casehub.eidos.api.ReactiveAgentStateStoreContractTest;
import org.junit.jupiter.api.BeforeEach;

class InMemoryReactiveAgentStateStoreTest extends ReactiveAgentStateStoreContractTest {

    private InMemoryReactiveAgentStateStore reactiveStore;

    @BeforeEach
    @Override
    protected void resetStore() {
        reactiveStore = new InMemoryReactiveAgentStateStore(new InMemoryAgentStateStore());
    }

    @Override
    protected ReactiveAgentStateStore store() {
        return reactiveStore;
    }
}
