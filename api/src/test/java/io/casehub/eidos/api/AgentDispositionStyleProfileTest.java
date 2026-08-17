package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AgentDispositionStyleProfileTest {

    @Test
    void styleProfile_defaults_to_empty_list() {
        var disposition = AgentDisposition.builder().build();
        assertThat(disposition.styleProfile()).isEmpty();
    }

    @Test
    void styleProfile_via_builder_varargs() {
        var disposition = AgentDisposition.builder()
                .styleProfile(DispositionValue.of("deadpan"))
                .build();
        assertThat(disposition.styleProfile()).hasSize(1);
        assertThat(disposition.styleProfile().getFirst().term()).isEqualTo("deadpan");
    }

    @Test
    void styleProfile_via_builder_list() {
        var disposition = AgentDisposition.builder()
                .styleProfile(List.of(
                        new DispositionValue("deadpan", 0.7),
                        new DispositionValue("brooding", 0.3)))
                .build();
        assertThat(disposition.styleProfile()).hasSize(2);
    }

    @Test
    void styleProfile_is_immutable() {
        var disposition = AgentDisposition.builder()
                .styleProfile(DispositionValue.of("deadpan"))
                .build();
        assertThatThrownBy(() -> disposition.styleProfile().add(DispositionValue.of("polite")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void descriptor_styleVocabulary_defaults_to_null() {
        var descriptor = AgentDescriptor.builder()
                .agentId("test").name("test").slot("test").tenancyId("t1").build();
        assertThat(descriptor.styleVocabulary()).isNull();
    }

    @Test
    void descriptor_styleVocabulary_set_via_builder() {
        var descriptor = AgentDescriptor.builder()
                .agentId("test").name("test").slot("test").tenancyId("t1")
                .styleVocabulary("urn:casehub:vocab:sarc7")
                .build();
        assertThat(descriptor.styleVocabulary()).isEqualTo("urn:casehub:vocab:sarc7");
    }
}
