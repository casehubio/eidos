package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDescriptorArchetypeTest {

    private static AgentDescriptor.Builder base() {
        return AgentDescriptor.builder()
            .agentId("test").name("Test Agent").slot("analyst")
            .tenancyId("t1");
    }

    @Test
    void archetypeDefaultsToNull() {
        var d = base().build();
        assertThat(d.archetype()).isNull();
        assertThat(d.archetypeAdjectives()).isEmpty();
    }

    @Test
    void archetypeCanBeSet() {
        var d = base()
            .archetype("detective")
            .archetypeAdjectives(List.of("meticulous", "persistent"))
            .build();
        assertThat(d.archetype()).isEqualTo("detective");
        assertThat(d.archetypeAdjectives()).containsExactly("meticulous", "persistent");
    }

    @Test
    void archetypeAdjectivesDefaultToEmptyList() {
        var d = base().archetype("detective").build();
        assertThat(d.archetypeAdjectives()).isEmpty();
    }

    @Test
    void archetypeAdjectivesWithoutArchetypeThrows() {
        assertThatThrownBy(() -> base()
            .archetypeAdjectives(List.of("meticulous"))
            .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("archetypeAdjectives");
    }

    @Test
    void archetypeAdjectivesAreImmutable() {
        var d = base()
            .archetype("detective")
            .archetypeAdjectives(List.of("meticulous"))
            .build();
        assertThatThrownBy(() -> d.archetypeAdjectives().add("another"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toBuilderPreservesArchetype() {
        var d = base()
            .archetype("detective")
            .archetypeAdjectives(List.of("meticulous"))
            .build();
        var copy = d.toBuilder().build();
        assertThat(copy.archetype()).isEqualTo("detective");
        assertThat(copy.archetypeAdjectives()).containsExactly("meticulous");
    }

    @Test
    void archetypeExceedingMaxLengthThrows() {
        assertThatThrownBy(() -> base()
            .archetype("x".repeat(101))
            .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("archetype");
    }

    @Test
    void archetypeAdjectiveTooManyThrows() {
        var adjectives = new java.util.ArrayList<String>();
        for (int i = 0; i < 11; i++) adjectives.add("adj" + i);
        assertThatThrownBy(() -> base()
            .archetype("detective")
            .archetypeAdjectives(adjectives)
            .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("archetypeAdjectives");
    }
}
