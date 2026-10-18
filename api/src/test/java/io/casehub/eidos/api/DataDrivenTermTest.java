package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DataDrivenTermTest {
    @Test void minimal_term() {
        var t = new DataDrivenTerm("test", "Test", "", List.of(), List.of(), List.of());
        assertThat(t.value()).isEqualTo("test");
        assertThat(t.label()).isEqualTo("Test");
        assertThat(t.keywords()).isEmpty();
        assertThat(t.specializes()).isEmpty();
    }

    @Test void null_coercion() {
        var t = new DataDrivenTerm("test", null, null, null, null, null);
        assertThat(t.label()).isEqualTo("");
        assertThat(t.description()).isEqualTo("");
        assertThat(t.aliases()).isEmpty();
        assertThat(t.keywords()).isEmpty();
        assertThat(t.specializes()).isEmpty();
    }

    @Test void blank_value_rejected() {
        assertThatThrownBy(() -> new DataDrivenTerm("", "L", "D", List.of(), List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void null_value_rejected() {
        assertThatThrownBy(() -> new DataDrivenTerm(null, "L", "D", List.of(), List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void banned_codepoint_rejected() {
        assertThatThrownBy(() -> new DataDrivenTerm("te​st", "L", "D", List.of(), List.of(), List.of()))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void blank_keyword_rejected() {
        assertThatThrownBy(() -> new DataDrivenTerm("t", "L", "D", List.of(), List.of("ok", ""), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void duplicate_keywords_deduplicated() {
        var t = new DataDrivenTerm("t", "L", "D", List.of(), List.of("a", "b", "a"), List.of());
        assertThat(t.keywords()).containsExactly("a", "b");
    }

    @Test void lists_are_immutable() {
        var t = new DataDrivenTerm("t", "L", "D", List.of("a1"), List.of("k1"), List.of());
        assertThatThrownBy(() -> t.keywords().add("k2")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> t.aliases().add("a2")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void specializes_returns_resolved_parents() {
        var parent = new DataDrivenTerm("parent", "Parent", "", List.of(), List.of(), List.of());
        var child = new DataDrivenTerm("child", "Child", "", List.of(), List.of(), List.of(parent));
        assertThat(child.specializes()).containsExactly(parent);
    }

    @Test void keywords_method_satisfies_vocabulary_term_contract() {
        var t = new DataDrivenTerm("t", "L", "D", List.of(), List.of("k1", "k2"), List.of());
        VocabularyTerm vt = t;
        assertThat(vt.keywords()).containsExactly("k1", "k2");
    }
}
