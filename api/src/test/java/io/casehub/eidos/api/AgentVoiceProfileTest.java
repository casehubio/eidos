package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentVoiceProfileTest {

    @Test void basicVoiceProfile() {
        var voice = new AgentVoiceProfile(
                "Penelope Pitstop's signature Southern belle — warm, effusive, delightfully oblivious",
                "southern-belle", "southern-drawl",
                List.of("Why, how delightful!", "Bless your heart!"),
                List.of("warm and effusive"),
                List.of("simply", "delightful"),
                List.of(),
                List.of("exclaims when discovering something new"),
                null);
        assertThat(voice.description()).isEqualTo("Penelope Pitstop's signature Southern belle — warm, effusive, delightfully oblivious");
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
                "Unctuous, overly helpful Sylvester Sneekly",
                "obsequious", null,
                List.of("Oh, my DEAR Miss Pitstop!"), List.of("overly helpful"),
                null, null, null, null);
        var claw = new AgentVoiceProfile(
                "Grandiose theatrical villain with dramatic monologues",
                "grandiose", null,
                List.of("Nyah-ha-ha-HA!"), List.of("dramatic monologues"),
                null, null, null, null);
        var personas = new LinkedHashMap<String, AgentVoiceProfile>();
        personas.put("sneekly", sneekly);
        personas.put("claw", claw);
        var voice = new AgentVoiceProfile(
                null, null, null, null, null, null, null,
                List.of("explains schemes even when alone"),
                personas);
        assertThat(voice.personas()).hasSize(2);
        assertThat(voice.personas().get("sneekly").register()).isEqualTo("obsequious");
        assertThat(voice.personas().get("claw").register()).isEqualTo("grandiose");
        assertThat(voice.quirks()).containsExactly("explains schemes even when alone");
    }

    @Test void resolvePersonaInheritsFromBase() {
        var casual = new AgentVoiceProfile(
                null, "informal", null, null, null, null, null, null, null);
        var base = new AgentVoiceProfile(
                "Formal British speaker", "formal", "received-pronunciation",
                null, List.of("measured and precise"),
                List.of("indeed"), null,
                List.of("pauses before speaking"),
                Map.of("casual", casual));
        var resolved = base.resolvePersona("casual");
        assertThat(resolved.description()).isEqualTo("Formal British speaker");
        assertThat(resolved.register()).isEqualTo("informal");
        assertThat(resolved.accent()).isEqualTo("received-pronunciation");
        assertThat(resolved.speechPatterns()).containsExactly("measured and precise");
        assertThat(resolved.vocabularyUses()).containsExactly("indeed");
        assertThat(resolved.quirks()).containsExactly("pauses before speaking");
        assertThat(resolved.personas()).isNull();
    }

    @Test void resolvePersonaInheritsDescriptionFromBase() {
        var persona = new AgentVoiceProfile(
                null, "casual", null, null, null, null, null, null, null);
        var base = new AgentVoiceProfile(
                "A dry wit", "formal", null, null, null, null, null, null,
                Map.of("casual", persona));
        var resolved = base.resolvePersona("casual");
        assertThat(resolved.description()).isEqualTo("A dry wit");
    }

    @Test void resolvePersonaOverridesDescription() {
        var persona = new AgentVoiceProfile(
                "Relaxed and chatty", "casual", null, null, null, null, null, null, null);
        var base = new AgentVoiceProfile(
                "Formal and precise", "formal", null, null, null, null, null, null,
                Map.of("casual", persona));
        var resolved = base.resolvePersona("casual");
        assertThat(resolved.description()).isEqualTo("Relaxed and chatty");
    }

    @Test void resolvePersonaReturnsBaseWhenNameNotFound() {
        var base = new AgentVoiceProfile(
                null, "formal", "rp", null, null, null, null, null, null);
        var resolved = base.resolvePersona("nonexistent");
        assertThat(resolved).isSameAs(base);
    }

    @Test void resolvePersonaReturnsBaseWhenNoPersonas() {
        var base = new AgentVoiceProfile(
                null, "formal", "rp", null, null, null, null, null, null);
        var resolved = base.resolvePersona("anything");
        assertThat(resolved).isSameAs(base);
    }

    @Test void defensiveCopiesAreImmutable() {
        var list = new java.util.ArrayList<>(List.of("a", "b"));
        var voice = new AgentVoiceProfile(null, null, null, list, null, null, null, null, null);
        list.add("c");
        assertThat(voice.catchphrases()).hasSize(2);
    }
}
