package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDescriptorAvatarTest {

    private static AgentDescriptor.Builder base() {
        return AgentDescriptor.builder()
            .agentId("test").name("Test").slot("analyst").tenancyId("t1");
    }

    @Test
    void avatarDefaultsToNull() {
        var d = base().build();
        assertThat(d.avatar()).isNull();
    }

    @Test
    void avatarCanBeSetToCollectionCode() {
        var d = base().avatar("mythic:P1B").build();
        assertThat(d.avatar()).isEqualTo("mythic:P1B");
    }

    @Test
    void avatarCanBeSetToExternalUrl() {
        var d = base().avatar("https://example.com/img.png").build();
        assertThat(d.avatar()).isEqualTo("https://example.com/img.png");
    }

    @Test
    void avatarExceedingMaxLengthThrows() {
        assertThatThrownBy(() -> base().avatar("x".repeat(501)).build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("avatar");
    }

    @Test
    void toBuilderPreservesAvatar() {
        var d = base().avatar("mythic:P1B").build();
        var copy = d.toBuilder().build();
        assertThat(copy.avatar()).isEqualTo("mythic:P1B");
    }
}
