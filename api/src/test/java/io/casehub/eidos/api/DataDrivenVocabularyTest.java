package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DataDrivenVocabularyTest {
    @Test void valid_vocabulary() {
        var t = new DataDrivenTerm("a", "A", "", List.of(), List.of(), List.of());
        var v = new DataDrivenVocabulary("urn:test", "Test", "1.0", "desc", List.of(t));
        assertThat(v.uri()).isEqualTo("urn:test");
        assertThat(v.terms()).hasSize(1);
    }

    @Test void blank_uri_rejected() {
        var t = new DataDrivenTerm("a", "A", "", List.of(), List.of(), List.of());
        assertThatThrownBy(() -> new DataDrivenVocabulary("", "n", "v", "d", List.of(t)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void empty_terms_rejected() {
        assertThatThrownBy(() -> new DataDrivenVocabulary("urn:test", "n", "v", "d", List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void null_terms_rejected() {
        assertThatThrownBy(() -> new DataDrivenVocabulary("urn:test", "n", "v", "d", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void duplicate_values_rejected() {
        var t1 = new DataDrivenTerm("a", "A", "", List.of(), List.of(), List.of());
        var t2 = new DataDrivenTerm("a", "B", "", List.of(), List.of(), List.of());
        assertThatThrownBy(() -> new DataDrivenVocabulary("urn:test", "n", "v", "d", List.of(t1, t2)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void null_coercion() {
        var t = new DataDrivenTerm("a", "A", "", List.of(), List.of(), List.of());
        var v = new DataDrivenVocabulary("urn:test", null, null, null, List.of(t));
        assertThat(v.name()).isEqualTo("");
        assertThat(v.version()).isEqualTo("");
        assertThat(v.description()).isEqualTo("");
    }

    @Test void terms_are_immutable() {
        var t = new DataDrivenTerm("a", "A", "", List.of(), List.of(), List.of());
        var v = new DataDrivenVocabulary("urn:test", "T", "1", "d", List.of(t));
        assertThatThrownBy(() -> v.terms().add(t)).isInstanceOf(UnsupportedOperationException.class);
    }
}
