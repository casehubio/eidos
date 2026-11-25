package io.casehub.eidos.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityOverlapRelevanceFactorTest {

    private final CapabilityOverlapRelevanceFactor factor = new CapabilityOverlapRelevanceFactor();

    private AttestationRelevanceContext ctx(Map<String, Object> snapshot) {
        return new AttestationRelevanceContext(
                UUID.randomUUID(), "attestor-1", ActorType.AGENT, "reviewer",
                "scored-1", "tenant-1", "code-review", null,
                Instant.now(), snapshot);
    }

    @Test
    void name_returns_capability_overlap() {
        assertThat(factor.name()).isEqualTo("capability-overlap");
    }

    @Test
    void null_snapshot_returns_neutral() {
        assertThat(factor.assess(ctx(null))).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void missing_key_returns_neutral() {
        assertThat(factor.assess(ctx(Map.of("other", 0.5)))).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void zero_overlap_returns_baseline() {
        var result = factor.assess(ctx(Map.of("capability-overlap", 0.0)));
        assertThat(result.weight()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void full_overlap_returns_one() {
        var result = factor.assess(ctx(Map.of("capability-overlap", 1.0)));
        assertThat(result.weight()).isEqualTo(1.0);
    }

    @Test
    void partial_overlap_returns_interpolated_weight() {
        var result = factor.assess(ctx(Map.of("capability-overlap", 0.5)));
        assertThat(result.weight()).isBetween(0.7, 0.8);
    }

    @Test
    void reason_includes_overlap_value() {
        var result = factor.assess(ctx(Map.of("capability-overlap", 0.75)));
        assertThat(result.reason()).contains("capability-overlap:0.75");
    }

    @Test
    void non_number_returns_neutral() {
        assertThat(factor.assess(ctx(Map.of("capability-overlap", "bad")))).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void integer_one_from_json_works() {
        var result = factor.assess(ctx(Map.of("capability-overlap", Integer.valueOf(1))));
        assertThat(result.weight()).isEqualTo(1.0);
    }
}
