package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderConfigTest {

    @Test
    void validConfig() {
        var pc = new ProviderConfig("claudony", Map.of("pool", "code-reviewer-pool", "command", "claude --model opus"));
        assertThat(pc.providerName()).isEqualTo("claudony");
        assertThat(pc.config()).containsEntry("pool", "code-reviewer-pool");
        assertThat(pc.config()).containsEntry("command", "claude --model opus");
    }

    @Test
    void nullConfigDefaultsToEmptyMap() {
        var pc = new ProviderConfig("claudony", null);
        assertThat(pc.config()).isEmpty();
    }

    @Test
    void configIsImmutableCopy() {
        var mutable = new java.util.HashMap<String, String>();
        mutable.put("pool", "test-pool");
        var pc = new ProviderConfig("claudony", mutable);
        assertThatThrownBy(() -> pc.config().put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullProviderNameThrows() {
        assertThatThrownBy(() -> new ProviderConfig(null, Map.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("providerName");
    }

    @Test
    void blankProviderNameThrows() {
        assertThatThrownBy(() -> new ProviderConfig("  ", Map.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("providerName");
    }
}
