package io.casehub.eidos.examples;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.DispositionValue;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CreativeDirectorAgentTest {

    @Inject
    AgentRegistry registry;

    @Test
    void hasVocabularyUris() {
        var d = registry.findById("creative-director-agent", "default").orElseThrow();
        assertThat(d.domainVocabulary()).isEqualTo("urn:casehub:vocab:conscientiousness");
        assertThat(d.slotVocabulary()).isEqualTo("urn:casehub:vocab:casehub-slot");
        assertThat(d.dispositionVocabulary()).isEqualTo("urn:casehub:vocab:jungian-function");
        assertThat(d.styleVocabulary()).isEqualTo("urn:casehub:vocab:sarc7");
    }

    @Test
    void hasDelegationEnabled() {
        var d = registry.findById("creative-director-agent", "default").orElseThrow();
        assertThat(d.disposition().delegation()).isTrue();
    }

    @Test
    void hasDispositionProfile() {
        var d = registry.findById("creative-director-agent", "default").orElseThrow();
        assertThat(d.disposition().dispositionProfile())
            .containsExactly(DispositionValue.of("EXTRAVERTED_INTUITION"), DispositionValue.of("INTROVERTED_FEELING"));
    }

    @Test
    void hasStyleProfile() {
        var d = registry.findById("creative-director-agent", "default").orElseThrow();
        assertThat(d.disposition().styleProfile())
            .containsExactly(DispositionValue.of("IRONY"), DispositionValue.of("ABSURDIST_HUMOUR"));
    }

    @Test
    void profileDrivenWithNoIndividualAxes() {
        var d = registry.findById("creative-director-agent", "default").orElseThrow();
        assertThat(d.disposition()).isNotNull();
        assertThat(d.capabilities()).hasSize(3);
    }
}
