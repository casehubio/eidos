package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.DispositionAxis;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CodeReviewAgentTest {

    @Inject
    AgentRegistry registry;

    @Test
    void isRegisteredWithProviderAndModelFamily() {
        var d = registry.findById("code-review-agent", "default").orElseThrow();
        assertThat(d.provider()).isEqualTo("anthropic");
        assertThat(d.modelFamily()).isEqualTo("claude-sonnet");
    }

    @Test
    void hasAllFiveDispositionAxes() {
        var d = registry.findById("code-review-agent", "default").orElseThrow();
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("direct");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("cautious");
        assertThat(d.disposition().primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("autonomous");
        assertThat(d.disposition().primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void hasCapabilities() {
        var d = registry.findById("code-review-agent", "default").orElseThrow();
        assertThat(d.capabilities()).hasSize(3);
        assertThat(d.capabilities().stream().map(c -> c.name()).toList())
            .containsExactly("code-review", "security-scan", "style-check");
    }
}
