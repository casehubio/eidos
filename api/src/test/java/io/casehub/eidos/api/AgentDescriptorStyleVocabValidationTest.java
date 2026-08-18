package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AgentDescriptorStyleVocabValidationTest {

    @Test
    void styleVocabularyWithBannedCharactersIsRejected() {
        assertThatThrownBy(() -> AgentDescriptor.builder()
                .agentId("test").name("Test").slot("test").tenancyId("t1")
                .styleVocabulary("urn:vocab​:bad")
                .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("styleVocabulary");
    }

    @Test
    void styleVocabularyExceedingMaxLengthIsRejected() {
        String tooLong = "urn:" + "x".repeat(500);
        assertThatThrownBy(() -> AgentDescriptor.builder()
                .agentId("test").name("Test").slot("test").tenancyId("t1")
                .styleVocabulary(tooLong)
                .build())
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("styleVocabulary");
    }

    @Test
    void validStyleVocabularyIsAccepted() {
        var d = AgentDescriptor.builder()
                .agentId("test").name("Test").slot("test").tenancyId("t1")
                .styleVocabulary("urn:casehub:vocab:style")
                .build();
        assertThat(d.styleVocabulary()).isEqualTo("urn:casehub:vocab:style");
    }

    @Test
    void nullStyleVocabularyIsAccepted() {
        var d = AgentDescriptor.builder()
                .agentId("test").name("Test").slot("test").tenancyId("t1")
                .build();
        assertThat(d.styleVocabulary()).isNull();
    }
}
