package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.ReactiveAgentRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactiveAgentDescriptorBootstrapTest {

    static AgentDescriptor desc(String agentId, String tenancyId) {
        return AgentDescriptor.builder()
            .agentId(agentId).name("N").slot("s").tenancyId(tenancyId).build();
    }

    @Test
    void registers_all_descriptors_reactively() {
        var registry = new ListReactiveRegistry();
        var registrars = List.<AgentDescriptorRegistrar>of(
            () -> List.of(desc("a1", "t1")),
            () -> List.of(desc("a2", "t1"))
        );

        ReactiveAgentDescriptorBootstrap.registerAll(registrars, registry)
            .await().indefinitely();
        assertThat(registry.registered).hasSize(2);
    }

    @Test
    void duplicate_agentId_tenancyId_pair_throws() {
        var registry = new ListReactiveRegistry();
        var registrars = List.<AgentDescriptorRegistrar>of(
            () -> List.of(desc("a1", "t1")),
            () -> List.of(desc("a1", "t1"))
        );

        assertThatThrownBy(() ->
            ReactiveAgentDescriptorBootstrap.registerAll(registrars, registry)
                .await().indefinitely())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate descriptor")
            .hasMessageContaining("a1")
            .hasMessageContaining("t1");
    }

    @Test
    void same_agentId_different_tenancy_is_allowed() {
        var registry = new ListReactiveRegistry();
        var registrars = List.<AgentDescriptorRegistrar>of(
            () -> List.of(desc("a1", "t1")),
            () -> List.of(desc("a1", "t2"))
        );

        ReactiveAgentDescriptorBootstrap.registerAll(registrars, registry)
            .await().indefinitely();
        assertThat(registry.registered).hasSize(2);
    }

    @Test
    void empty_registrars_registers_nothing() {
        var registry = new ListReactiveRegistry();
        ReactiveAgentDescriptorBootstrap.registerAll(List.of(), registry)
            .await().indefinitely();
        assertThat(registry.registered).isEmpty();
    }

    static class ListReactiveRegistry implements ReactiveAgentRegistry {
        final List<AgentDescriptor> registered = new ArrayList<>();

        @Override public Uni<Void> register(AgentDescriptor d) {
            registered.add(d);
            return Uni.createFrom().voidItem();
        }
        @Override public Uni<Optional<AgentDescriptor>> findById(String id, String tid) {
            return Uni.createFrom().item(Optional.empty());
        }
        @Override public Uni<List<AgentMatch>> find(AgentQuery q) {
            return Uni.createFrom().item(List.of());
        }
    }
}
