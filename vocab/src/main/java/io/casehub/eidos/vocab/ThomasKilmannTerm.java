package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:thomas-kilmann",
                    name = "Thomas-Kilmann Conflict Modes", version = "1.0",
                    description = "Five conflict-handling modes from the Thomas-Kilmann Conflict Mode Instrument, based on the assertiveness × cooperativeness framework. Widely adopted in management and applied psychology. Maps to the CONFLICT_MODE disposition axis.")
public enum ThomasKilmannTerm implements VocabularyTerm {

    COMPETING     ("competing",     "Competing",
        "High assertiveness, low cooperativeness; pursues own position in conflict",
        List.of("competitive")),
    COLLABORATING ("collaborating", "Collaborating",
        "High assertiveness, high cooperativeness; seeks joint problem-solving",
        List.of("cooperative", "collaborative")),
    COMPROMISING  ("compromising",  "Compromising",
        "Moderate assertiveness and cooperativeness; neither fully assertive nor yielding",
        List.of("compromise")),
    AVOIDING      ("avoiding",      "Avoiding",
        "Low assertiveness, low cooperativeness; sidesteps conflict",
        List.of("avoidant")),
    ACCOMMODATING ("accommodating", "Accommodating",
        "Low assertiveness, high cooperativeness; yields to others' concerns",
        List.of("deferring"));

    public static final String URI = "urn:casehub:vocab:thomas-kilmann";

    private final String value, label, description;
    private final List<String> aliases;

    ThomasKilmannTerm(String value, String label, String description, List<String> aliases) {
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
