package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.DispositionAxis;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class DocumentAnalystTest {

    @Inject
    AgentRegistry registry;

    @Test
    void documentAnalystIsRegistered() {
        var d = registry.findById("document-analyst", "default").orElseThrow();
        assertThat(d.slot()).isEqualTo("document-analyst");
        assertThat(d.briefing()).isEqualTo("Analyses documents and extracts key findings");
        assertThat(d.disposition().primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("moderate");
        assertThat(d.disposition().primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("cautious");
    }
}
