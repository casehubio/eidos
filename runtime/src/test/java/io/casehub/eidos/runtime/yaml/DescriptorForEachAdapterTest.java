package io.casehub.eidos.runtime.yaml;

import io.casehub.yaml.core.resolver.VariableResolver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptorForEachAdapterTest {

    private final DescriptorForEachAdapter adapter = new DescriptorForEachAdapter();

    @Test
    void getId_returns_agentId() {
        var map = Map.<String, Object>of("agentId", "test-agent", "name", "Test");
        assertThat(adapter.getId(map)).isEqualTo("test-agent");
    }

    @Test
    void getForEach_returns_forEach_value() {
        var map = Map.<String, Object>of("agentId", "a", "forEach", "teams");
        assertThat(adapter.getForEach(map)).isEqualTo("teams");
    }

    @Test
    void getForEach_returns_null_when_absent() {
        var map = Map.<String, Object>of("agentId", "a");
        assertThat(adapter.getForEach(map)).isNull();
    }

    @Test
    void getWhen_returns_when_value() {
        var map = Map.<String, Object>of("agentId", "a", "when", "${var.enabled}");
        assertThat(adapter.getWhen(map)).isEqualTo("${var.enabled}");
    }

    @Test
    void getWhen_returns_null_when_absent() {
        var map = Map.<String, Object>of("agentId", "a");
        assertThat(adapter.getWhen(map)).isNull();
    }

    @Test
    void stamp_resolves_variables_in_map() {
        var template = new LinkedHashMap<String, Object>();
        template.put("agentId", "${each.team}-reviewer");
        template.put("name", "${each.team} Reviewer");
        template.put("slot", "reviewer");

        var resolver = new VariableResolver(Map.of(), Set.of())
                .withEachContext(Map.of("team", "frontend"));

        var result = adapter.stamp(template, "tpl.frontend", resolver);
        assertThat(result.get("agentId")).isEqualTo("frontend-reviewer");
        assertThat(result.get("name")).isEqualTo("frontend Reviewer");
        assertThat(result.get("slot")).isEqualTo("reviewer");
    }

    @Test
    void stamp_strips_forEach_and_when() {
        var template = new LinkedHashMap<String, Object>();
        template.put("agentId", "a");
        template.put("forEach", "teams");
        template.put("when", "${var.enabled}");

        var resolver = new VariableResolver(Map.of(), Set.of());
        var result = adapter.stamp(template, "a", resolver);

        assertThat(result).doesNotContainKey("forEach");
        assertThat(result).doesNotContainKey("when");
        assertThat(result).containsKey("agentId");
    }

    @Test
    void stamp_resolves_nested_map_values() {
        var caps = new LinkedHashMap<String, Object>();
        caps.put("name", "${each.team}-review");
        var template = new LinkedHashMap<String, Object>();
        template.put("agentId", "a");
        template.put("capability", caps);

        var resolver = new VariableResolver(Map.of(), Set.of())
                .withEachContext(Map.of("team", "backend"));

        var result = adapter.stamp(template, "a.backend", resolver);
        @SuppressWarnings("unchecked")
        var resolvedCap = (Map<String, Object>) result.get("capability");
        assertThat(resolvedCap.get("name")).isEqualTo("backend-review");
    }

    @Test
    void stamp_does_not_mutate_template() {
        var template = new LinkedHashMap<String, Object>();
        template.put("agentId", "${each.x}");
        template.put("forEach", "group");

        var resolver = new VariableResolver(Map.of(), Set.of())
                .withEachContext(Map.of("x", "val"));

        adapter.stamp(template, "tpl.val", resolver);

        assertThat(template.get("agentId")).isEqualTo("${each.x}");
        assertThat(template).containsKey("forEach");
    }
}
