package io.casehub.eidos.eval;

import io.casehub.eidos.eval.FunctionActivationJudge.FunctionScenario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionScenarioLoaderTest {

    @Test
    void loadsAllEightFunctions() {
        Map<String, List<FunctionScenario>> scenarios = FunctionScenarioLoader.load();
        assertThat(scenarios).hasSize(8);
        assertThat(scenarios.keySet()).containsExactlyInAnyOrder(
            "ti", "te", "fi", "fe", "si", "se", "ni", "ne");
    }

    @Test
    void eachFunctionHasThreeScenarios() {
        Map<String, List<FunctionScenario>> scenarios = FunctionScenarioLoader.load();
        for (var entry : scenarios.entrySet()) {
            assertThat(entry.getValue())
                .as("scenarios for " + entry.getKey())
                .hasSize(3);
        }
    }

    @Test
    void scenarioTargetFunctionMatchesKey() {
        Map<String, List<FunctionScenario>> scenarios = FunctionScenarioLoader.load();
        for (var entry : scenarios.entrySet()) {
            for (var s : entry.getValue()) {
                assertThat(s.targetFunction()).isEqualTo(entry.getKey());
            }
        }
    }
}
