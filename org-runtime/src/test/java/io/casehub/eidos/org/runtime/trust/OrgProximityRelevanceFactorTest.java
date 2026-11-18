package io.casehub.eidos.org.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgProximityRelevanceFactorTest {

    private static final List<Double> DEFAULT_WEIGHTS = List.of(1.0, 0.7, 0.65, 0.6);

    private OrgProximityRelevanceFactor factor(List<Double> weights) {
        return new OrgProximityRelevanceFactor(weights);
    }

    private AttestationRelevanceContext ctx(Map<String, Object> snapshot) {
        return new AttestationRelevanceContext(
                UUID.randomUUID(), "attestor-1", ActorType.AGENT, "reviewer",
                "scored-1", "tenant-1", "code-review", null,
                Instant.now(), snapshot);
    }

    @Test
    void name_returns_org_proximity() {
        assertThat(factor(DEFAULT_WEIGHTS).name()).isEqualTo("org-proximity");
    }

    @Test
    void null_snapshot_returns_neutral() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(null));
        assertThat(result).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void missing_org_distance_key_returns_neutral() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("other-key", 42)));
        assertThat(result).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void negative_one_distance_returns_neutral() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", -1)));
        assertThat(result).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void distance_1_returns_first_weight() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 1)));
        assertThat(result.weight()).isEqualTo(1.0);
        assertThat(result.reason()).contains("org-distance:1");
    }

    @Test
    void distance_2_returns_second_weight() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 2)));
        assertThat(result.weight()).isEqualTo(0.7);
        assertThat(result.reason()).contains("org-distance:2");
    }

    @Test
    void distance_3_returns_third_weight() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 3)));
        assertThat(result.weight()).isEqualTo(0.65);
    }

    @Test
    void distance_4_returns_last_weight() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 4)));
        assertThat(result.weight()).isEqualTo(0.6);
    }

    @Test
    void distance_beyond_table_repeats_last_entry() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 10)));
        assertThat(result.weight()).isEqualTo(0.6);
        assertThat(result.reason()).contains("org-distance:10");
    }

    @Test
    void distance_0_self_attestation_returns_neutral() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 0)));
        assertThat(result).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void custom_weight_table() {
        var custom = List.of(0.9, 0.5);
        var result = factor(custom).assess(ctx(Map.of("org-distance", 1)));
        assertThat(result.weight()).isEqualTo(0.9);

        result = factor(custom).assess(ctx(Map.of("org-distance", 2)));
        assertThat(result.weight()).isEqualTo(0.5);

        result = factor(custom).assess(ctx(Map.of("org-distance", 5)));
        assertThat(result.weight()).isEqualTo(0.5);
    }

    @Test
    void empty_weight_table_rejected() {
        assertThatThrownBy(() -> factor(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weight_out_of_range_rejected() {
        assertThatThrownBy(() -> factor(List.of(1.5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factor(List.of(-0.1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void integer_distance_from_json_deserialization() {
        // Jackson may deserialize integers as Integer, not int
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", Integer.valueOf(2))));
        assertThat(result.weight()).isEqualTo(0.7);
    }

    @Test
    void long_distance_from_json_deserialization() {
        // Jackson may deserialize large numbers as Long
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 3L)));
        assertThat(result.weight()).isEqualTo(0.65);
    }

    @Test
    void reason_includes_weight() {
        var result = factor(DEFAULT_WEIGHTS).assess(ctx(Map.of("org-distance", 2)));
        assertThat(result.reason()).contains("weight:0.7");
    }
}
