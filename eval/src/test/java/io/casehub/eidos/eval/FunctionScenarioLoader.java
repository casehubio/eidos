package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.eval.FunctionActivationJudge.FunctionScenario;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FunctionScenarioLoader {

    private FunctionScenarioLoader() {}

    private static final ObjectMapper YAML =
        new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @SuppressWarnings("unchecked")
    static Map<String, List<FunctionScenario>> load() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("function-scenarios/scenarios.yaml")) {
            if (is == null) throw new IllegalStateException("function-scenarios/scenarios.yaml not found");
            final Map<String, Object> root = YAML.readValue(is, Map.class);
            final List<Map<String, Object>> rawScenarios = (List<Map<String, Object>>) root.get("scenarios");
            final Map<String, List<FunctionScenario>> result = new LinkedHashMap<>();
            for (final Map<String, Object> entry : rawScenarios) {
                final String fn = (String) entry.get("targetFunction");
                final List<String> prompts = (List<String>) entry.get("prompts");
                result.put(fn, prompts.stream()
                    .map(p -> new FunctionScenario(fn, p))
                    .toList());
            }
            return result;
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load function-scenarios/scenarios.yaml", e);
        }
    }
}
