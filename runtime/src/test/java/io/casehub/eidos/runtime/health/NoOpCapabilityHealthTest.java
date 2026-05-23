package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilityHealth;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class NoOpCapabilityHealthTest {

    @Inject
    CapabilityHealth health;

    @Test
    void probe_always_returns_ready() {
        var status = health.probe("any-agent", "any-capability",
            CapabilityHealth.ProbeContext.of("any-domain"));

        assertThat(status).isInstanceOf(CapabilityHealth.CapabilityStatus.Ready.class);
    }
}
