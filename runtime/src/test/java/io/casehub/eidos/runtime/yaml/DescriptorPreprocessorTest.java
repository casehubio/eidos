package io.casehub.eidos.runtime.yaml;

import io.casehub.yaml.core.resolver.VariableSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptorPreprocessorTest {

    static Map<String, Object> descriptor(String agentId) {
        return descriptor(agentId, Map.of());
    }

    static Map<String, Object> descriptor(String agentId, Map<String, Object> extras) {
        var map = new LinkedHashMap<String, Object>();
        map.put("agentId", agentId);
        map.put("name", agentId);
        map.put("slot", "worker");
        map.put("tenancyId", "default");
        map.putAll(extras);
        return map;
    }

    @Test
    void passthrough_no_preprocessing_keys() {
        var raw = Map.<String, Object>of(
                "descriptors", List.of(descriptor("agent-1"), descriptor("agent-2")));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("agent-1");
        assertThat(result.get(1).get("agentId")).isEqualTo("agent-2");
    }

    @Test
    void variable_resolution_in_descriptor_fields() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("variables", Map.of("tenant", "acme"));
        raw.put("descriptors", List.of(
                descriptor("${var.tenant}-reviewer", Map.of(
                        "tenancyId", "${var.tenant}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("agentId")).isEqualTo("acme-reviewer");
        assertThat(result.get(0).get("tenancyId")).isEqualTo("acme");
    }

    @Test
    void forEach_named_group_expands() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("iterations", Map.of("teams",
                Map.of("as", "team", "in", List.of("frontend", "backend"))));
        raw.put("descriptors", List.of(
                descriptor("${each.team}-reviewer", Map.of("forEach", "teams"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("frontend-reviewer");
        assertThat(result.get(1).get("agentId")).isEqualTo("backend-reviewer");
    }

    @Test
    void forEach_inline_expands() {
        var forEach = Map.<String, Object>of(
                "as", "env", "in", List.of("dev", "prod"));
        var raw = Map.<String, Object>of(
                "descriptors", List.of(
                        descriptor("${each.env}-agent", Map.of("forEach", forEach))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("dev-agent");
        assertThat(result.get(1).get("agentId")).isEqualTo("prod-agent");
    }

    @Test
    void forEach_strips_forEach_key_from_result() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("iterations", Map.of("teams",
                Map.of("as", "team", "in", List.of("a"))));
        raw.put("descriptors", List.of(
                descriptor("${each.team}", Map.of("forEach", "teams"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result.get(0)).doesNotContainKey("forEach");
    }

    @Test
    void mixed_static_and_forEach_preserves_order() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("iterations", Map.of("env",
                Map.of("as", "e", "in", List.of("a", "b"))));
        var descs = new ArrayList<Map<String, Object>>();
        descs.add(descriptor("first"));
        descs.add(descriptor("${each.e}-expand", Map.of("forEach", "env")));
        descs.add(descriptor("last"));
        raw.put("descriptors", descs);

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(4);
        assertThat(result.stream().map(m -> m.get("agentId")).toList())
                .containsExactly("first", "a-expand", "b-expand", "last");
    }

    @Test
    void expansion_limit_exceeded_throws() {
        var values = new ArrayList<String>();
        for (int i = 0; i < 101; i++) values.add("v" + i);
        var forEach = Map.<String, Object>of("as", "x", "in", values);
        var raw = Map.<String, Object>of(
                "descriptors", List.of(
                        descriptor("${each.x}", Map.of("forEach", forEach))));

        assertThatThrownBy(() ->
                DescriptorPreprocessor.preprocess(raw, Map.of(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("101")
                .hasMessageContaining("100");
    }

    @Test
    void unresolved_variable_throws() {
        var raw = Map.<String, Object>of(
                "descriptors", List.of(descriptor("${var.missing}")));

        assertThatThrownBy(() ->
                DescriptorPreprocessor.preprocess(raw, Map.of(), null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void external_variable_source_resolved() {
        VariableSource configSource = name ->
                "db.host".equals(name) ? "localhost" : null;
        var raw = Map.<String, Object>of(
                "descriptors", List.of(
                        descriptor("${config.db.host}-agent")));

        var result = DescriptorPreprocessor.preprocess(
                raw, Map.of("config", configSource), null);

        assertThat(result.get(0).get("agentId")).isEqualTo("localhost-agent");
    }

    @Test
    void variables_combined_with_forEach() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("variables", Map.of("org", "acme"));
        raw.put("iterations", Map.of("teams",
                Map.of("as", "team", "in", List.of("fe", "be"))));
        raw.put("descriptors", List.of(
                descriptor("${var.org}-${each.team}",
                        Map.of("forEach", "teams",
                                "tenancyId", "${var.org}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("acme-fe");
        assertThat(result.get(0).get("tenancyId")).isEqualTo("acme");
        assertThat(result.get(1).get("agentId")).isEqualTo("acme-be");
    }

    @Test
    void empty_descriptors_returns_empty() {
        var raw = Map.<String, Object>of("descriptors", List.of());
        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);
        assertThat(result).isEmpty();
    }

    @Test
    void null_descriptors_returns_empty() {
        var raw = Map.<String, Object>of();
        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);
        assertThat(result).isEmpty();
    }

    // --- when conditions ---

    @Test
    void when_true_includes_descriptor() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("variables", Map.of("enabled", "true"));
        raw.put("descriptors", List.of(
                descriptor("gated", Map.of("when", "${var.enabled}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("agentId")).isEqualTo("gated");
        assertThat(result.get(0)).doesNotContainKey("when");
    }

    @Test
    void when_false_excludes_descriptor() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("variables", Map.of("enabled", "false"));
        raw.put("descriptors", List.of(
                descriptor("gated", Map.of("when", "${var.enabled}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void when_with_forEach_per_copy_exclusion() {
        var forEach = Map.<String, Object>of(
                "as", "flag", "in", List.of("true", "false"));
        var raw = Map.<String, Object>of(
                "descriptors", List.of(
                        descriptor("${each.flag}-agent",
                                Map.of("forEach", forEach, "when", "${each.flag}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("agentId")).isEqualTo("true-agent");
    }

    // --- CSV data sources ---

    @Test
    void csv_inline_expansion() {
        var csvContent = "name:STRING, role:STRING\nalice, reviewer\nbob, planner";
        var raw = new LinkedHashMap<String, Object>();
        raw.put("dataSources", Map.of("roster",
                Map.of("csv", csvContent)));
        var forEach = Map.<String, Object>of("as", "agent", "in", "roster");
        raw.put("descriptors", List.of(
                descriptor("${each.agent.name}-agent",
                        Map.of("forEach", forEach,
                                "slot", "${each.agent.role}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("alice-agent");
        assertThat(result.get(0).get("slot")).isEqualTo("reviewer");
        assertThat(result.get(1).get("agentId")).isEqualTo("bob-agent");
        assertThat(result.get(1).get("slot")).isEqualTo("planner");
    }

    @Test
    void csv_classpath_file_expansion() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("dataSources", Map.of("roster",
                Map.of("file", "io/casehub/eidos/runtime/yaml/test-agents.csv")));
        var forEach = Map.<String, Object>of("as", "agent", "in", "roster");
        raw.put("descriptors", List.of(
                descriptor("${each.agent.name}-agent",
                        Map.of("forEach", forEach,
                                "slot", "${each.agent.role}"))));

        var result = DescriptorPreprocessor.preprocess(
                raw, Map.of(), Thread.currentThread().getContextClassLoader());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("agentId")).isEqualTo("carol-agent");
        assertThat(result.get(0).get("slot")).isEqualTo("analyst");
    }

    @Test
    void csv_with_when_excludes_rows() {
        var csvContent = "name:STRING, active:BOOLEAN\nalice, true\nbob, false";
        var raw = new LinkedHashMap<String, Object>();
        raw.put("dataSources", Map.of("roster",
                Map.of("csv", csvContent)));
        var forEach = Map.<String, Object>of("as", "agent", "in", "roster");
        raw.put("descriptors", List.of(
                descriptor("${each.agent.name}-agent",
                        Map.of("forEach", forEach,
                                "when", "${each.agent.active}"))));

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("agentId")).isEqualTo("alice-agent");
    }

    @Test
    void csv_expansion_limit_exceeded_throws() {
        var sb = new StringBuilder("name:STRING\n");
        for (int i = 0; i < 101; i++) sb.append("v").append(i).append("\n");
        var raw = new LinkedHashMap<String, Object>();
        raw.put("dataSources", Map.of("big",
                Map.of("csv", sb.toString())));
        var forEach = Map.<String, Object>of("as", "row", "in", "big");
        raw.put("descriptors", List.of(
                descriptor("${each.row.name}", Map.of("forEach", forEach))));

        assertThatThrownBy(() ->
                DescriptorPreprocessor.preprocess(raw, Map.of(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("101")
                .hasMessageContaining("100");
    }

    @Test
    void datasource_and_iteration_namespace_collision_throws() {
        var raw = new LinkedHashMap<String, Object>();
        raw.put("iterations", Map.of("shared",
                Map.of("as", "x", "in", List.of("a"))));
        raw.put("dataSources", Map.of("shared",
                Map.of("csv", "name:STRING\nalice")));
        raw.put("descriptors", List.of(descriptor("a")));

        assertThatThrownBy(() ->
                DescriptorPreprocessor.preprocess(raw, Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shared");
    }

    @Test
    void mixed_csv_and_normal_forEach_preserves_order() {
        var csvContent = "name:STRING\nalice\nbob";
        var raw = new LinkedHashMap<String, Object>();
        raw.put("dataSources", Map.of("people",
                Map.of("csv", csvContent)));
        raw.put("iterations", Map.of("env",
                Map.of("as", "e", "in", List.of("dev", "prod"))));
        var descs = new ArrayList<Map<String, Object>>();
        descs.add(descriptor("first"));
        descs.add(descriptor("${each.person.name}-agent",
                Map.of("forEach", Map.of("as", "person", "in", "people"))));
        descs.add(descriptor("${each.e}-env",
                Map.of("forEach", "env")));
        descs.add(descriptor("last"));
        raw.put("descriptors", descs);

        var result = DescriptorPreprocessor.preprocess(raw, Map.of(), null);

        assertThat(result.stream().map(m -> m.get("agentId")).toList())
                .containsExactly("first", "alice-agent", "bob-agent",
                        "dev-env", "prod-env", "last");
    }
}
