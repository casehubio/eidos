package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTierTermTest {

    @Test
    void flagshipSpecializesStandard() {
        assertThat(ModelTierTerm.FLAGSHIP.specializes()).containsExactly(ModelTierTerm.STANDARD);
    }

    @Test
    void standardSpecializesFast() {
        assertThat(ModelTierTerm.STANDARD.specializes()).containsExactly(ModelTierTerm.FAST);
    }

    @Test
    void fastDoesNotSpecialize() {
        assertThat(ModelTierTerm.FAST.specializes()).isEmpty();
    }

    @Test
    void embeddingDoesNotSpecialize() {
        assertThat(ModelTierTerm.EMBEDDING.specializes()).isEmpty();
    }

    @Test
    void uriIsCorrect() {
        assertThat(ModelTierTerm.URI).isEqualTo("urn:casehub:vocab:model-tier");
    }

    @Test
    void allTermsHaveValues() {
        for (var term : ModelTierTerm.values()) {
            assertThat(term.value()).isNotBlank();
            assertThat(term.label()).isNotBlank();
            assertThat(term.description()).isNotBlank();
        }
    }

    @Test
    void linearChainDepth() {
        VocabularyTerm current = ModelTierTerm.FLAGSHIP;
        int depth = 0;
        while (!current.specializes().isEmpty()) {
            current = current.specializes().getFirst();
            depth++;
        }
        assertThat(depth).isEqualTo(2);
        assertThat(current).isEqualTo(ModelTierTerm.FAST);
    }

    @Test
    void valuesAreLowercase() {
        for (var term : ModelTierTerm.values()) {
            assertThat(term.value()).isEqualTo(term.value().toLowerCase());
        }
    }
}
