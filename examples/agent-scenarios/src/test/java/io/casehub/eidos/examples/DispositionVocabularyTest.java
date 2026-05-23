package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.vocab.ConscientiousnessVocabularyProducer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class DispositionVocabularyTest {

    @Inject VocabularyRegistry vocabRegistry;

    static final String VOCAB = ConscientiousnessVocabularyProducer.URI;

    @Test
    void resolve_rule_following_axis_values() {
        assertThat(vocabRegistry.resolve(VOCAB, "strict")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "principled")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "flexible")).isPresent();

        var strict = vocabRegistry.resolve(VOCAB, "strict").get();
        assertThat(strict.label()).isEqualTo("Strict Rule Following");
        assertThat(strict.aliases()).contains("rule-bound");
    }

    @Test
    void resolve_risk_appetite_axis_values() {
        assertThat(vocabRegistry.resolve(VOCAB, "conservative")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "measured")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "bold")).isPresent();

        var bold = vocabRegistry.resolve(VOCAB, "bold").get();
        assertThat(bold.aliases()).contains("risk-tolerant");
    }

    @Test
    void resolve_social_orient_axis_values() {
        assertThat(vocabRegistry.resolve(VOCAB, "collaborative")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "independent")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "facilitative")).isPresent();
    }

    @Test
    void resolve_autonomy_axis_values() {
        assertThat(vocabRegistry.resolve(VOCAB, "directed")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "semi-autonomous")).isPresent();
        assertThat(vocabRegistry.resolve(VOCAB, "autonomous")).isPresent();

        var autonomous = vocabRegistry.resolve(VOCAB, "autonomous").get();
        assertThat(autonomous.aliases()).contains("self-governing", "agentic");
    }

    @Test
    void resolve_by_alias() {
        var term = vocabRegistry.resolve(VOCAB, "risk-averse");
        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("conservative");
    }
}
