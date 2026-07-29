package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyTermTest {

    @Test
    void defaultProfile_returns_empty_list_by_default() {
        VocabularyTerm term = new VocabularyTerm() {
            @Override public String value() { return "test"; }
            @Override public String label() { return "Test"; }
        };
        assertThat(term.defaultProfile()).isEmpty();
    }
}
