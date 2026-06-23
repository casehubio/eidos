package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDescriptorBootstrapTest {

    static AgentDescriptor desc(String agentId, String tenancyId) {
        return AgentDescriptor.builder()
            .agentId(agentId).name("N").slot("s").tenancyId(tenancyId).build();
    }

    @Test
    void duplicate_agentId_tenancyId_pair_throws() {
        var registrars = List.<AgentDescriptorRegistrar>of(
            () -> List.of(desc("a1", "t1")),
            () -> List.of(desc("a1", "t1"))
        );

        assertThatThrownBy(() -> AgentDescriptorBootstrap.registerAll(registrars, new ListRegistry()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate descriptor")
            .hasMessageContaining("a1")
            .hasMessageContaining("t1");
    }

    @Test
    void same_agentId_different_tenancy_is_allowed() {
        var registry = new ListRegistry();
        var registrars = List.<AgentDescriptorRegistrar>of(
            () -> List.of(desc("a1", "t1")),
            () -> List.of(desc("a1", "t2"))
        );

        AgentDescriptorBootstrap.registerAll(registrars, registry);
        assertThat(registry.registered).hasSize(2);
    }

    @Test
    void empty_registrars_registers_nothing() {
        var registry = new ListRegistry();
        AgentDescriptorBootstrap.registerAll(List.of(), registry);
        assertThat(registry.registered).isEmpty();
    }

    static class ListRegistry implements AgentRegistry {
        final List<AgentDescriptor> registered = new ArrayList<>();

        @Override public void register(AgentDescriptor d) { registered.add(d); }
        @Override public java.util.Optional<AgentDescriptor> findById(String id, String tid) {
            return java.util.Optional.empty();
        }
        @Override public List<AgentDescriptor> find(io.casehub.eidos.api.AgentQuery q) {
            return List.of();
        }
    }
}
