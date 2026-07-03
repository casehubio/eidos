package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class AgentRegistrySpiTest {

    @Test
    void anonymous_implementation_satisfies_contract() {
        AgentRegistry registry = new AgentRegistry() {
            @Override public void register(AgentDescriptor d) {}
            @Override public Optional<AgentDescriptor> findById(String id, String tenancyId) { return Optional.empty(); }
            @Override public List<AgentMatch> find(AgentQuery q) { return List.of(); }
        };
        assertThat(registry.findById("x", "default")).isEmpty();
        assertThat(registry.find(AgentQuery.all("default"))).isEmpty();
    }
}
