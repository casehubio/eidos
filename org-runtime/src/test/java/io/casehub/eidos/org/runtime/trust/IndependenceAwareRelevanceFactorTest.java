package io.casehub.eidos.org.runtime.trust;

import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.ledger.api.spi.RelevanceAssessment;
import io.casehub.platform.api.identity.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class IndependenceAwareRelevanceFactorTest {

    private static final double DEFAULT_CAP = 0.6;

    private IndependenceAwareRelevanceFactor factor(double maxSameUnitShare) {
        return new IndependenceAwareRelevanceFactor(maxSameUnitShare);
    }

    private AttestationRelevanceContext ctx(UUID id, Map<String, Object> snapshot) {
        return new AttestationRelevanceContext(
                id, "attestor-" + id.toString().substring(0, 4), ActorType.AGENT, "reviewer",
                "scored-1", "tenant-1", "code-review", null,
                Instant.now(), snapshot);
    }

    private AttestationRelevanceContext sameUnitCtx(UUID id) {
        return ctx(id, Map.of("org-distance", 1));
    }

    private AttestationRelevanceContext crossUnitCtx(UUID id, int distance) {
        return ctx(id, Map.of("org-distance", distance));
    }

    private AttestationRelevanceContext unknownCtx(UUID id) {
        return ctx(id, null);
    }

    @Test
    void name_returns_independence() {
        assertThat(factor(DEFAULT_CAP).name()).isEqualTo("independence");
    }

    @Test
    void assess_per_item_returns_neutral() {
        var result = factor(DEFAULT_CAP).assess(sameUnitCtx(UUID.randomUUID()));
        assertThat(result).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void under_cap_all_neutral() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 3; i++) contexts.add(sameUnitCtx(ids.get(i)));
        for (int i = 3; i < 10; i++) contexts.add(crossUnitCtx(ids.get(i), 2));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        for (UUID id : ids) {
            assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void over_cap_same_unit_downweighted() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 8; i++) contexts.add(sameUnitCtx(ids.get(i)));
        for (int i = 8; i < 10; i++) contexts.add(crossUnitCtx(ids.get(i), 3));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        // w = 0.6 * 2 / (0.4 * 8) = 0.375
        for (int i = 0; i < 8; i++) {
            assertThat(result.get(ids.get(i)).weight()).isCloseTo(0.375, within(1e-9));
        }
        for (int i = 8; i < 10; i++) {
            assertThat(result.get(ids.get(i))).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void exactly_at_cap_all_neutral() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 6; i++) contexts.add(sameUnitCtx(ids.get(i)));
        for (int i = 6; i < 10; i++) contexts.add(crossUnitCtx(ids.get(i), 2));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        for (UUID id : ids) {
            assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void all_same_unit_all_neutral() {
        var ids = uuids(5);
        var contexts = ids.stream().map(this::sameUnitCtx).toList();

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        for (UUID id : ids) {
            assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void all_cross_unit_all_neutral() {
        var ids = uuids(5);
        var contexts = ids.stream().map(id -> crossUnitCtx(id, 3)).toList();

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        for (UUID id : ids) {
            assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void unknowns_excluded_from_share_and_get_neutral() {
        var ids = uuids(12);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 8; i++) contexts.add(sameUnitCtx(ids.get(i)));
        for (int i = 8; i < 10; i++) contexts.add(crossUnitCtx(ids.get(i), 2));
        for (int i = 10; i < 12; i++) contexts.add(unknownCtx(ids.get(i)));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        // Share computed on 8+2=10 known, not 12 total
        // w = 0.6 * 2 / (0.4 * 8) = 0.375
        for (int i = 0; i < 8; i++) {
            assertThat(result.get(ids.get(i)).weight()).isCloseTo(0.375, within(1e-9));
        }
        for (int i = 8; i < 10; i++) {
            assertThat(result.get(ids.get(i))).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
        for (int i = 10; i < 12; i++) {
            assertThat(result.get(ids.get(i))).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void empty_batch_returns_empty() {
        var result = factor(DEFAULT_CAP).assessBatch(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void single_attestation_neutral() {
        UUID id = UUID.randomUUID();
        var result = factor(DEFAULT_CAP).assessBatch(List.of(sameUnitCtx(id)));
        assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void extreme_imbalance_weight_correct() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 9; i++) contexts.add(sameUnitCtx(ids.get(i)));
        contexts.add(crossUnitCtx(ids.get(9), 2));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        // w = 0.6 * 1 / (0.4 * 9) = 0.1666...
        double expected = 0.6 / (0.4 * 9);
        for (int i = 0; i < 9; i++) {
            assertThat(result.get(ids.get(i)).weight()).isCloseTo(expected, within(1e-9));
        }
        assertThat(result.get(ids.get(9))).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void reason_includes_share_and_weight() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 8; i++) contexts.add(sameUnitCtx(ids.get(i)));
        for (int i = 8; i < 10; i++) contexts.add(crossUnitCtx(ids.get(i), 2));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        assertThat(result.get(ids.get(0)).reason()).contains("same-unit 8/10");
        assertThat(result.get(ids.get(0)).reason()).contains("cap 60%");
    }

    @Test
    void cap_zero_rejected() {
        assertThatThrownBy(() -> factor(0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cap_above_one_rejected() {
        assertThatThrownBy(() -> factor(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cap_nan_rejected() {
        assertThatThrownBy(() -> factor(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void cap_one_accepted_disables_factor() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 9; i++) contexts.add(sameUnitCtx(ids.get(i)));
        contexts.add(crossUnitCtx(ids.get(9), 2));

        var result = factor(1.0).assessBatch(contexts);

        for (UUID id : ids) {
            assertThat(result.get(id)).isEqualTo(RelevanceAssessment.NEUTRAL);
        }
    }

    @Test
    void missing_key_treated_as_unknown() {
        var ids = uuids(10);
        var contexts = new ArrayList<AttestationRelevanceContext>();
        for (int i = 0; i < 8; i++) contexts.add(sameUnitCtx(ids.get(i)));
        contexts.add(ctx(ids.get(8), Map.of("other-key", 42)));
        contexts.add(crossUnitCtx(ids.get(9), 2));

        var result = factor(DEFAULT_CAP).assessBatch(contexts);

        // Share: 8 same / (8 same + 1 cross) = 88.9% > 60%
        // w = 0.6 * 1 / (0.4 * 8) = 0.1875
        double expected = 0.6 / (0.4 * 8);
        for (int i = 0; i < 8; i++) {
            assertThat(result.get(ids.get(i)).weight()).isCloseTo(expected, within(1e-9));
        }
        assertThat(result.get(ids.get(8))).isEqualTo(RelevanceAssessment.NEUTRAL);
        assertThat(result.get(ids.get(9))).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void distance_zero_treated_as_unknown() {
        UUID sameUnit = UUID.randomUUID();
        UUID selfAttest = UUID.randomUUID();
        UUID cross = UUID.randomUUID();

        var result = factor(DEFAULT_CAP).assessBatch(List.of(
                sameUnitCtx(sameUnit),
                ctx(selfAttest, Map.of("org-distance", 0)),
                crossUnitCtx(cross, 2)));

        // 1 same, 1 cross, 1 unknown (distance 0) -> share = 1/2 = 50% <= 60%
        assertThat(result.get(sameUnit)).isEqualTo(RelevanceAssessment.NEUTRAL);
        assertThat(result.get(selfAttest)).isEqualTo(RelevanceAssessment.NEUTRAL);
        assertThat(result.get(cross)).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    @Test
    void negative_distance_treated_as_unknown() {
        UUID sameUnit = UUID.randomUUID();
        UUID negative = UUID.randomUUID();

        var result = factor(DEFAULT_CAP).assessBatch(List.of(
                sameUnitCtx(sameUnit),
                ctx(negative, Map.of("org-distance", -1))));

        // 1 same, 0 cross, 1 unknown -> crossCount == 0 -> all NEUTRAL
        assertThat(result.get(sameUnit)).isEqualTo(RelevanceAssessment.NEUTRAL);
        assertThat(result.get(negative)).isEqualTo(RelevanceAssessment.NEUTRAL);
    }

    private List<UUID> uuids(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
    }
}
