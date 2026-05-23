package io.casehub.eidos.vocab;

import io.casehub.eidos.api.Vocabulary;
import io.casehub.eidos.api.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ConscientiousnessVocabularyProducer {

    public static final String URI = "urn:casehub:vocab:conscientiousness";

    @Produces
    @ApplicationScoped
    public Vocabulary conscientiousnessVocabulary() {
        return new Vocabulary(URI, "Conscientiousness Disposition Axes", "1.0", Map.ofEntries(
            Map.entry("strict",          new VocabularyTerm("strict", "Strict Rule Following",
                "Follows rules rigidly", List.of("rule-bound", "compliant"), Map.of())),
            Map.entry("principled",      new VocabularyTerm("principled", "Principled",
                "Follows intent of rules", List.of("values-based"), Map.of())),
            Map.entry("flexible",        new VocabularyTerm("flexible", "Flexible",
                "Adapts rules to context", List.of("adaptive", "pragmatic"), Map.of())),
            Map.entry("conservative",    new VocabularyTerm("conservative", "Conservative Risk",
                "Avoids uncertainty", List.of("risk-averse", "cautious"), Map.of())),
            Map.entry("measured",        new VocabularyTerm("measured", "Measured Risk",
                "Balances risk and reward", List.of("balanced"), Map.of())),
            Map.entry("bold",            new VocabularyTerm("bold", "Bold Risk",
                "Accepts high uncertainty for reward", List.of("risk-tolerant", "adventurous"), Map.of())),
            Map.entry("collaborative",   new VocabularyTerm("collaborative", "Collaborative",
                "Works with others by default", List.of("team-oriented", "cooperative"), Map.of())),
            Map.entry("independent",     new VocabularyTerm("independent", "Independent",
                "Works alone by preference", List.of("autonomous-social", "self-directed"), Map.of())),
            Map.entry("facilitative",    new VocabularyTerm("facilitative", "Facilitative",
                "Enables others to work", List.of("supportive", "enabling"), Map.of())),
            Map.entry("directed",        new VocabularyTerm("directed", "Directed Autonomy",
                "Follows explicit instructions", List.of("instruction-following"), Map.of())),
            Map.entry("semi-autonomous", new VocabularyTerm("semi-autonomous", "Semi-Autonomous",
                "Acts within defined boundaries", List.of("bounded-autonomy"), Map.of())),
            Map.entry("autonomous",      new VocabularyTerm("autonomous", "Autonomous",
                "Acts on own judgment", List.of("self-governing", "agentic"), Map.of()))
        ));
    }
}
