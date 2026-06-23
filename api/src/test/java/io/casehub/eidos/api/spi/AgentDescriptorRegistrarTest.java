package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.AgentDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDescriptorRegistrarTest {

    @Test
    void registrar_returns_descriptors() {
        AgentDescriptorRegistrar registrar = () -> List.of(
            AgentDescriptor.builder()
                .agentId("test-1").name("Test").slot("tester").tenancyId("t")
                .build()
        );

        var descriptors = registrar.descriptors();
        assertThat(descriptors).hasSize(1);
        assertThat(descriptors.get(0).agentId()).isEqualTo("test-1");
    }

    @Test
    void registrar_is_functional_interface() {
        assertThat(AgentDescriptorRegistrar.class.isAnnotationPresent(FunctionalInterface.class))
            .isTrue();
    }
}
