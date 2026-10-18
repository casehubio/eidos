package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VocabularyInfoTest {
    @Test void creates_with_all_fields() {
        var info = new VocabularyInfo("urn:test", "Test", "1.0", "desc");
        assertThat(info.uri()).isEqualTo("urn:test");
        assertThat(info.name()).isEqualTo("Test");
        assertThat(info.version()).isEqualTo("1.0");
        assertThat(info.description()).isEqualTo("desc");
    }

    @Test void null_coercion() {
        var info = new VocabularyInfo("urn:test", null, null, null);
        assertThat(info.name()).isEqualTo("");
        assertThat(info.version()).isEqualTo("");
        assertThat(info.description()).isEqualTo("");
    }

    @Test void blank_uri_rejected() {
        assertThatThrownBy(() -> new VocabularyInfo("", "n", "v", "d"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void null_uri_rejected() {
        assertThatThrownBy(() -> new VocabularyInfo(null, "n", "v", "d"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
