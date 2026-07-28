package io.casehub.eidos.api;

import io.casehub.eidos.api.DispositionHealth.DispositionStatus;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus.Aligned;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus.Drifted;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus.EvolutionPending;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DispositionHealthTest {

    @Test
    void aligned_construction_and_access() {
        var weights = Map.of("ti", 0.35, "ne", 0.20);
        var status = new Aligned(weights);
        assertThat(status.effectiveWeights()).isEqualTo(weights);
    }

    @Test
    void drifted_construction_and_access() {
        var weights = Map.of("ti", 0.30, "ne", 0.25);
        var status = new Drifted(weights, "ne", 0.12);
        assertThat(status.effectiveWeights()).isEqualTo(weights);
        assertThat(status.mostActivated()).isEqualTo("ne");
        assertThat(status.driftMagnitude()).isEqualTo(0.12);
    }

    @Test
    void evolution_pending_construction_and_access() {
        EvolutionType type = () -> "TEST_TYPE";
        var weights = Map.of("ti", 0.25, "ne", 0.30);
        var status = new EvolutionPending(type, "ne", weights);
        assertThat(status.type().name()).isEqualTo("TEST_TYPE");
        assertThat(status.candidateFunction()).isEqualTo("ne");
        assertThat(status.effectiveWeights()).isEqualTo(weights);
    }

    @Test
    void pattern_matching_aligned() {
        DispositionStatus status = new Aligned(Map.of());
        var result = switch (status) {
            case Aligned a -> "aligned:" + a.effectiveWeights().size();
            case Drifted d -> "drifted:" + d.driftMagnitude();
            case EvolutionPending e -> "pending:" + e.candidateFunction();
        };
        assertThat(result).isEqualTo("aligned:0");
    }

    @Test
    void pattern_matching_drifted() {
        DispositionStatus status = new Drifted(Map.of("ti", 0.40), "ti", 0.05);
        var result = switch (status) {
            case Aligned a -> "aligned";
            case Drifted d -> "drifted:" + d.mostActivated();
            case EvolutionPending e -> "pending";
        };
        assertThat(result).isEqualTo("drifted:ti");
    }

    @Test
    void pattern_matching_evolution_pending() {
        EvolutionType type = () -> "SWAP";
        DispositionStatus status = new EvolutionPending(type, "ne", Map.of());
        var result = switch (status) {
            case Aligned a -> "aligned";
            case Drifted d -> "drifted";
            case EvolutionPending e -> "pending:" + e.type().name();
        };
        assertThat(result).isEqualTo("pending:SWAP");
    }

    @Test
    void aligned_is_disposition_status() {
        assertThat(new Aligned(Map.of())).isInstanceOf(DispositionStatus.class);
    }

    @Test
    void drifted_is_disposition_status() {
        assertThat(new Drifted(Map.of(), "x", 0.0)).isInstanceOf(DispositionStatus.class);
    }

    @Test
    void evolution_pending_is_disposition_status() {
        EvolutionType type = () -> "T";
        assertThat(new EvolutionPending(type, "x", Map.of()))
                .isInstanceOf(DispositionStatus.class);
    }
}
