package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:conscientiousness",
                    name = "Conscientiousness Disposition Axes", version = "1.0")
public enum ConscientiousnessTerm implements VocabularyTerm {

    // RULE_FOLLOWING axis
    STRICT      ("strict",         "Strict Rule Following",
                 "Follows rules rigidly",              List.of("rule-bound", "compliant")),
    PRINCIPLED  ("principled",     "Principled",
                 "Follows intent of rules",             List.of("values-based")),
    FLEXIBLE    ("flexible",       "Flexible",
                 "Adapts rules to context",             List.of("adaptive", "pragmatic")),

    // RISK_APPETITE axis
    CONSERVATIVE("conservative",   "Conservative Risk",
                 "Avoids uncertainty",                  List.of("risk-averse", "cautious")),
    MEASURED    ("measured",       "Measured Risk",
                 "Balances risk and reward",            List.of("balanced")),
    BOLD        ("bold",           "Bold Risk",
                 "Accepts uncertainty for reward",      List.of("risk-tolerant", "adventurous")),

    // SOCIAL_ORIENTATION axis
    COLLABORATIVE("collaborative", "Collaborative",
                 "Works with others by default",        List.of("team-oriented", "cooperative")),
    INDEPENDENT ("independent",    "Independent",
                 "Works alone by preference",           List.of("autonomous-social", "self-directed")),
    FACILITATIVE("facilitative",   "Facilitative",
                 "Enables others to work",              List.of("supportive", "enabling")),

    // AUTONOMY axis
    DIRECTED    ("directed",       "Directed Autonomy",
                 "Follows explicit instructions",       List.of("instruction-following")),
    SEMI_AUTONOMOUS("semi-autonomous", "Semi-Autonomous",
                 "Acts within defined boundaries",      List.of("bounded-autonomy")),
    AUTONOMOUS  ("autonomous",     "Autonomous",
                 "Acts on own judgment",                List.of("self-governing", "agentic"));

    public static final String URI = "urn:casehub:vocab:conscientiousness";

    private final String value, label, description;
    private final List<String> aliases;

    ConscientiousnessTerm(String value, String label, String description, List<String> aliases) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.aliases = aliases;
    }

    @Override public String value()         { return value; }
    @Override public String label()         { return label; }
    @Override public String description()   { return description; }
    @Override public List<String> aliases() { return aliases; }
}
