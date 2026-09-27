package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentVoiceProfileTest {

    @Test void basicVoiceProfile() {
        var voice = new AgentVoiceProfile(
                "southern-belle", "southern-drawl",
                List.of("Why, how delightful!", "Bless your heart!"),
                List.of("warm and effusive"),
                List.of("simply", "delightful"),
                List.of(),
                List.of("exclaims when discovering something new"),
                null);
        assertThat(voice.register()).isEqualTo("southern-belle");
        assertThat(voice.accent()).isEqualTo("southern-drawl");
        assertThat(voice.catchphrases()).hasSize(2);
        assertThat(voice.speechPatterns()).containsExactly("warm and effusive");
        assertThat(voice.vocabularyUses()).containsExactly("simply", "delightful");
        assertThat(voice.vocabularyAvoids()).isEmpty();
        assertThat(voice.quirks()).containsExactly("exclaims when discovering something new");
        assertThat(voice.personas()).isNull();
    }

    @Test void personasMapPreserved() {
        var sneekly = new AgentVoiceProfile(
                "obsequious", null,
                List.of("Oh, my DEAR Miss Pitstop!"), List.of("overly helpful"),
                null, null, null, null);
        var claw = new AgentVoiceProfile(
                "grandiose", null,
                List.of("Nyah-ha-ha-HA!"), List.of("dramatic monologues"),
                null, null, null, null);
        var personas = new LinkedHashMap<String, AgentVoiceProfile>();
        personas.put("sneekly", sneekly);
        personas.put("claw", claw);
        var voice = new AgentVoiceProfile(
                null, null, null, null, null, null,
                List.of("explains schemes even when alone"),
                personas);
        assertThat(voice.personas()).hasSize(2);
        assertThat(voice.personas().get("sneekly").register()).isEqualTo("obsequious");
        assertThat(voice.personas().get("claw").register()).isEqualTo("grandiose");
        assertThat(voice.quirks()).containsExactly("explains schemes even when alone");
    }

    @Test void resolvePersonaInheritsFromBase() {
        var casual = new AgentVoiceProfile(
                "informal", null, null, null, null, null, null, null);
        var base = new AgentVoiceProfile(
                "formal", "received-pronunciation",
                null, List.of("measured and precise"),
                List.of("indeed"), null,
                List.of("pauses before speaking"),
                Map.of("casual", casual));
        var resolved = base.resolvePersona("casual");
        assertThat(resolved.register()).isEqualTo("informal");
        assertThat(resolved.accent()).isEqualTo("received-pronunciation");
        assertThat(resolved.speechPatterns()).containsExactly("measured and precise");
        assertThat(resolved.vocabularyUses()).containsExactly("indeed");
        assertThat(resolved.quirks()).containsExactly("pauses before speaking");
        assertThat(resolved.personas()).isNull();
    }

    @Test void resolvePersonaReturnsBaseWhenNameNotFound() {
        var base = new AgentVoiceProfile(
                "formal", "rp", null, null, null, null, null, null);
        var resolved = base.resolvePersona("nonexistent");
        assertThat(resolved).isSameAs(base);
    }

    @Test void resolvePersonaReturnsBaseWhenNoPersonas() {
        var base = new AgentVoiceProfile(
                "formal", "rp", null, null, null, null, null, null);
        var resolved = base.resolvePersona("anything");
        assertThat(resolved).isSameAs(base);
    }

    @Test void defensiveCopiesAreImmutable() {
        var list = new java.util.ArrayList<>(List.of("a", "b"));
        var voice = new AgentVoiceProfile(null, null, list, null, null, null, null, null);
        list.add("c");
        assertThat(voice.catchphrases()).hasSize(2);
    }
}
