package io.casehub.eidos.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JungianProfileLoaderTest {

    @Test
    void loadsAllProfilesFromIndex() {
        var profiles = new JungianProfileLoader().load();
        assertThat(profiles).hasSize(8);
        assertThat(profiles).extracting(JungianProfile::mbtiType)
            .containsExactlyInAnyOrder("INTP", "ENTJ", "INFP", "ENFJ", "ISTJ", "ESTP", "INTJ", "ENTP");
    }

    @Test
    void profileHasRequiredFields() {
        var profiles = new JungianProfileLoader().load();
        var intp = profiles.stream().filter(p -> "INTP".equals(p.mbtiType())).findFirst().orElseThrow();
        assertThat(intp.name()).isEqualTo("intp-analyst");
        assertThat(intp.role()).isEqualTo("Systems Analyst — Ti dominant");
        assertThat(intp.dominantFunction()).isEqualTo("ti");
        assertThat(intp.auxiliaryFunction()).isEqualTo("ne");
        assertThat(intp.descriptor()).isNotNull();
        assertThat(intp.descriptor().briefing()).isNotBlank();
        assertThat(intp.descriptor().disposition().dispositionProfile()).isNotEmpty();
        assertThat(intp.descriptor().dispositionVocabulary()).isEqualTo("urn:casehub:vocab:jungian");
    }

    @Test
    void allProfilesHaveDominantAndAuxiliary() {
        var profiles = new JungianProfileLoader().load();
        for (var p : profiles) {
            assertThat(p.dominantFunction()).as(p.name() + " dominant").isNotBlank();
            assertThat(p.auxiliaryFunction()).as(p.name() + " auxiliary").isNotBlank();
        }
    }
}
