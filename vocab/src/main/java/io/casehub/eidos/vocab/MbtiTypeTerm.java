package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.Arrays;
import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:mbti",
                    name = "MBTI Types via Jungian Specialization", version = "1.0",
                    description = "Sixteen MBTI personality types, each grounded in Jungian cognitive functions via specializes(). The type label is an emergent property of the weighted function stack — not an injected identity.")
public enum MbtiTypeTerm implements VocabularyTerm {

    INTJ("intj", "INTJ — Architect",   "Ni-Te: strategic visionary with systematic execution",    JungianFunctionTerm.NI, JungianFunctionTerm.TE),
    INTP("intp", "INTP — Logician",    "Ti-Ne: analytical thinker with possibility exploration",  JungianFunctionTerm.TI, JungianFunctionTerm.NE),
    ENTJ("entj", "ENTJ — Commander",   "Te-Ni: decisive organizer with strategic insight",        JungianFunctionTerm.TE, JungianFunctionTerm.NI),
    ENTP("entp", "ENTP — Debater",     "Ne-Ti: innovative explorer with logical analysis",        JungianFunctionTerm.NE, JungianFunctionTerm.TI),
    INFJ("infj", "INFJ — Advocate",    "Ni-Fe: insightful idealist with social harmony",          JungianFunctionTerm.NI, JungianFunctionTerm.FE),
    INFP("infp", "INFP — Mediator",    "Fi-Ne: values-driven explorer with authentic expression", JungianFunctionTerm.FI, JungianFunctionTerm.NE),
    ENFJ("enfj", "ENFJ — Protagonist", "Fe-Ni: empathetic leader with visionary insight",         JungianFunctionTerm.FE, JungianFunctionTerm.NI),
    ENFP("enfp", "ENFP — Campaigner",  "Ne-Fi: enthusiastic explorer with deep personal values",  JungianFunctionTerm.NE, JungianFunctionTerm.FI),
    ISTJ("istj", "ISTJ — Logistician", "Si-Te: reliable organizer with proven methods",           JungianFunctionTerm.SI, JungianFunctionTerm.TE),
    ISTP("istp", "ISTP — Virtuoso",    "Ti-Se: practical analyst with hands-on engagement",       JungianFunctionTerm.TI, JungianFunctionTerm.SE),
    ESTJ("estj", "ESTJ — Executive",   "Te-Si: efficient administrator with established process", JungianFunctionTerm.TE, JungianFunctionTerm.SI),
    ESTP("estp", "ESTP — Entrepreneur","Se-Ti: action-oriented pragmatist with analytical edge",  JungianFunctionTerm.SE, JungianFunctionTerm.TI),
    ISFJ("isfj", "ISFJ — Defender",    "Si-Fe: dedicated supporter with interpersonal warmth",    JungianFunctionTerm.SI, JungianFunctionTerm.FE),
    ISFP("isfp", "ISFP — Adventurer",  "Fi-Se: gentle artist with present-moment awareness",      JungianFunctionTerm.FI, JungianFunctionTerm.SE),
    ESFJ("esfj", "ESFJ — Consul",     "Fe-Si: caring organizer with loyalty to tradition",       JungianFunctionTerm.FE, JungianFunctionTerm.SI),
    ESFP("esfp", "ESFP — Entertainer", "Se-Fi: spontaneous performer with personal authenticity", JungianFunctionTerm.SE, JungianFunctionTerm.FI);

    public static final String URI = "urn:casehub:vocab:mbti";

    private static final double DOM_WEIGHT = 0.35;
    private static final double AUX_WEIGHT = 0.20;
    private static final double REMAINING_TOTAL = 1.0 - DOM_WEIGHT - AUX_WEIGHT;

    private final String value, label, description;
    private final JungianFunctionTerm dominant, auxiliary;

    MbtiTypeTerm(String value, String label, String description,
                 JungianFunctionTerm dominant, JungianFunctionTerm auxiliary) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.dominant = dominant;
        this.auxiliary = auxiliary;
    }

    @Override public String value()       { return value; }
    @Override public String label()       { return label; }
    @Override public String description() { return description; }

    @Override
    public List<VocabularyTerm> specializes() {
        return List.of(dominant, auxiliary);
    }

    public List<DispositionValue> defaultProfile() {
        JungianFunctionTerm[] others = Arrays.stream(JungianFunctionTerm.values())
                .filter(f -> f != dominant && f != auxiliary)
                .toArray(JungianFunctionTerm[]::new);
        double perOther = REMAINING_TOTAL / others.length;

        var profile = new java.util.ArrayList<DispositionValue>();
        profile.add(new DispositionValue(dominant.value(), DOM_WEIGHT));
        profile.add(new DispositionValue(auxiliary.value(), AUX_WEIGHT));
        for (var fn : others) {
            profile.add(new DispositionValue(fn.value(), Math.round(perOther * 10000.0) / 10000.0));
        }

        double sum = profile.stream().mapToDouble(DispositionValue::weight).sum();
        if (Math.abs(sum - 1.0) > 0.001) {
            var last = profile.removeLast();
            profile.add(new DispositionValue(last.term(), Math.round((last.weight() + 1.0 - sum) * 10000.0) / 10000.0));
        }

        return List.copyOf(profile);
    }
}
