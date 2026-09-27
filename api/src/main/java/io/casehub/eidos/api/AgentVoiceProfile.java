package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;

public record AgentVoiceProfile(
        String register,
        String accent,
        List<String> catchphrases,
        List<String> speechPatterns,
        List<String> vocabularyUses,
        List<String> vocabularyAvoids,
        List<String> quirks,
        Map<String, AgentVoiceProfile> personas
) {
    public AgentVoiceProfile {
        catchphrases     = catchphrases != null ? List.copyOf(catchphrases) : null;
        speechPatterns   = speechPatterns != null ? List.copyOf(speechPatterns) : null;
        vocabularyUses   = vocabularyUses != null ? List.copyOf(vocabularyUses) : null;
        vocabularyAvoids = vocabularyAvoids != null ? List.copyOf(vocabularyAvoids) : null;
        quirks           = quirks != null ? List.copyOf(quirks) : null;
        personas         = personas != null ? Map.copyOf(personas) : null;
    }

    public AgentVoiceProfile resolvePersona(String personaName) {
        if (personas == null || !personas.containsKey(personaName)) {
            return this;
        }
        var persona = personas.get(personaName);
        return new AgentVoiceProfile(
                persona.register() != null ? persona.register() : this.register,
                persona.accent() != null ? persona.accent() : this.accent,
                persona.catchphrases() != null ? persona.catchphrases() : this.catchphrases,
                persona.speechPatterns() != null ? persona.speechPatterns() : this.speechPatterns,
                persona.vocabularyUses() != null ? persona.vocabularyUses() : this.vocabularyUses,
                persona.vocabularyAvoids() != null ? persona.vocabularyAvoids() : this.vocabularyAvoids,
                persona.quirks() != null ? persona.quirks() : this.quirks,
                null
        );
    }
}
