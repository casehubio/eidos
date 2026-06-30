package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MatchDegreeTest {

    @Test
    void exact_has_no_depth() {
        var exact = new MatchDegree.Exact();
        assertThat(exact).isInstanceOf(MatchDegree.class);
    }

    @Test
    void plugin_carries_depth() {
        var plugin = new MatchDegree.Plugin(2);
        assertThat(plugin.depth()).isEqualTo(2);
    }

    @Test
    void specialization_carries_depth() {
        var spec = new MatchDegree.Specialization(3);
        assertThat(spec.depth()).isEqualTo(3);
    }

    @Test
    void none_is_singleton_value() {
        assertThat(new MatchDegree.None()).isEqualTo(new MatchDegree.None());
    }

    @Test
    void exhaustive_switch_compiles() {
        MatchDegree degree = new MatchDegree.Plugin(1);
        String result = switch (degree) {
            case MatchDegree.Exact e -> "exact";
            case MatchDegree.Plugin p -> "plugin:" + p.depth();
            case MatchDegree.Specialization s -> "spec:" + s.depth();
            case MatchDegree.None n -> "none";
        };
        assertThat(result).isEqualTo("plugin:1");
    }
}
