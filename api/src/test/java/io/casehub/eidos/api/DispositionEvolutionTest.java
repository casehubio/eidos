package io.casehub.eidos.api;

import io.casehub.eidos.api.DispositionEvolution.EvolutionResult;
import io.casehub.eidos.api.DispositionEvolution.EvolutionResult.Dampened;
import io.casehub.eidos.api.DispositionEvolution.EvolutionResult.Evolved;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DispositionEvolutionTest {

    @Test
    void evolved_construction_and_access() {
        var profile = List.of(
                new DispositionValue("ne", 0.35),
                new DispositionValue("ti", 0.20));
        var result = new Evolved(profile, "INTP", "ENTP");
        assertThat(result.newProfile()).isEqualTo(profile);
        assertThat(result.previousTypeLabel()).isEqualTo("INTP");
        assertThat(result.newTypeLabel()).isEqualTo("ENTP");
    }

    @Test
    void dampened_construction_and_access() {
        var result = new Dampened(0.20);
        assertThat(result.decayFactor()).isEqualTo(0.20);
    }

    @Test
    void pattern_matching_evolved() {
        EvolutionResult result = new Evolved(List.of(), "A", "B");
        var output = switch (result) {
            case Evolved e -> "evolved:" + e.previousTypeLabel() + "->" + e.newTypeLabel();
            case Dampened d -> "dampened:" + d.decayFactor();
        };
        assertThat(output).isEqualTo("evolved:A->B");
    }

    @Test
    void pattern_matching_dampened() {
        EvolutionResult result = new Dampened(0.15);
        var output = switch (result) {
            case Evolved e -> "evolved";
            case Dampened d -> "dampened:" + d.decayFactor();
        };
        assertThat(output).isEqualTo("dampened:0.15");
    }

    @Test
    void evolved_is_evolution_result() {
        assertThat(new Evolved(List.of(), "X", "Y"))
                .isInstanceOf(EvolutionResult.class);
    }

    @Test
    void dampened_is_evolution_result() {
        assertThat(new Dampened(0.2))
                .isInstanceOf(EvolutionResult.class);
    }
}
