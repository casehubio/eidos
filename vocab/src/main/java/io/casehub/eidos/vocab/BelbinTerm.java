package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:belbin",
                    name = "Belbin Team Roles", version = "1.0")
public enum BelbinTerm implements VocabularyTerm {

    PLANT                ("plant",                "Plant",
        "Creative, unorthodox problem-solver; generates novel ideas independently",
        List.of("pl")),
    RESOURCE_INVESTIGATOR("resource-investigator","Resource Investigator",
        "Extrovert who explores external opportunities and develops contacts",
        List.of("ri")),
    CO_ORDINATOR         ("co-ordinator",         "Co-ordinator",
        "Clarifies goals, promotes team decision-making, delegates effectively",
        List.of("co")),
    SHAPER               ("shaper",               "Shaper",
        "Challenges the team to improve; driven, dynamic, thrives under pressure",
        List.of("sh")),
    MONITOR_EVALUATOR    ("monitor-evaluator",    "Monitor Evaluator",
        "Sober, strategic, discerning; sees all options and judges accurately",
        List.of("me")),
    TEAMWORKER           ("teamworker",           "Teamworker",
        "Cooperative, perceptive, diplomatic; averts friction and builds cohesion",
        List.of("tw")),
    IMPLEMENTER          ("implementer",          "Implementer",
        "Disciplined, reliable, efficient; turns ideas into practical actions",
        List.of("imp")),
    COMPLETER_FINISHER   ("completer-finisher",   "Completer Finisher",
        "Painstaking, conscientious, anxious; ensures delivery to standard",
        List.of("cf")),
    SPECIALIST           ("specialist",           "Specialist",
        "Dedicated, self-starting, single-minded; provides rare knowledge",
        List.of("sp"));

    public static final String URI = "urn:casehub:vocab:belbin";

    private final String value, label, description;
    private final List<String> aliases;

    BelbinTerm(String value, String label, String description, List<String> aliases) {
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
