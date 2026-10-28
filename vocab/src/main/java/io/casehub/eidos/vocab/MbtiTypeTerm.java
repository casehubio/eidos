package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
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

    @Override
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

    @Override
    public double axisSignalWeight(DispositionAxis axis) {
        double e = value.charAt(0) == 'e' ? 1 : -1;
        double n = value.charAt(1) == 'n' ? 1 : -1;
        double t = value.charAt(2) == 't' ? 1 : -1;
        double j = value.charAt(3) == 'j' ? 1 : -1;
        return switch (axis) {
            case SOCIAL_ORIENTATION -> e * 0.7;
            case RULE_FOLLOWING -> j * 0.6;
            case RISK_APPETITE -> n * 0.5;
            case AUTONOMY -> e > 0 ? -0.3 : 0.4;
            case CONFLICT_MODE -> t * 0.6;
        };
    }

    @Override
    public String adjective() {
        return switch (this) {
            case INTJ -> "analytical and strategic";
            case INTP -> "logical and inventive";
            case ENTJ -> "commanding and decisive";
            case ENTP -> "innovative and resourceful";
            case INFJ -> "insightful and principled";
            case INFP -> "idealistic and empathetic";
            case ENFJ -> "charismatic and mentoring";
            case ENFP -> "enthusiastic and imaginative";
            case ISTJ -> "methodical and dependable";
            case ISFJ -> "nurturing and conscientious";
            case ESTJ -> "organized and authoritative";
            case ESFJ -> "supportive and community-minded";
            case ISTP -> "pragmatic and adaptable";
            case ISFP -> "artistic and sensitive";
            case ESTP -> "bold and action-oriented";
            case ESFP -> "spontaneous and engaging";
        };
    }

    @Override
    public String detailedDescription() {
        return switch (this) {
            case INTJ -> "INTJs combine long-range vision with analytical rigour. They see systems and patterns, build mental models, and work relentlessly to improve them. In teams, they contribute strategic thinking and high standards but may struggle with small talk and consensus-building. They prefer written communication, value competence over credentials, and become frustrated by inefficiency or illogical processes.";
            case INTP -> "INTPs are driven by understanding how things work at a fundamental level. They excel at finding logical inconsistencies, building frameworks, and exploring possibilities. In work settings, they contribute deep analysis and novel approaches but may resist structure and deadlines. They think by talking through ideas, value precision in language, and are energised by intellectual challenge.";
            case ENTJ -> "ENTJs see inefficiency as a personal challenge. They naturally organise people, systems, and processes toward goals. In teams, they drive execution, make quick decisions, and hold people accountable. They can be perceived as blunt or impatient, but their directness often accelerates progress. They value strategic thinking and become frustrated by indecision or lack of follow-through.";
            case ENTP -> "ENTPs thrive on intellectual exploration and debate. They generate ideas rapidly, see connections others miss, and enjoy dismantling conventional thinking. In teams, they contribute innovation and creative problem-solving but may start more projects than they finish. They are energised by brainstorming, value intellectual freedom, and resist routine or bureaucracy.";
            case INFJ -> "INFJs combine deep intuition about people with a drive to create meaningful change. They see potential in individuals and systems, and work persistently toward their vision. In teams, they contribute empathy, strategic insight, and moral clarity, but need time alone to process. They are energised by purposeful work and frustrated by superficiality or ethical compromise.";
            case INFP -> "INFPs are guided by deeply held values and a vision of how things could be. They bring creativity, empathy, and genuine care to their work. In teams, they contribute emotional intelligence and original thinking but may avoid confrontation. They are energised by meaningful work aligned with their values and frustrated by environments that feel inauthentic.";
            case ENFJ -> "ENFJs are driven to help others grow and reach their potential. They read group dynamics intuitively, build consensus, and inspire action. In teams, they contribute warmth, vision, and strong communication, but may overextend themselves caring for others. They are energised by collaborative achievement and frustrated by apathy or interpersonal conflict.";
            case ENFP -> "ENFPs bring infectious energy and a talent for connecting ideas and people. They see potential everywhere, generate enthusiasm for new directions, and champion causes they believe in. In teams, they contribute creativity, warmth, and adaptability, but may resist detailed follow-through. They thrive in dynamic environments and struggle with rigid processes.";
            case ISTJ -> "ISTJs are the backbone of reliable execution. They follow through on commitments, maintain standards, and build systems that work consistently. In teams, they contribute thoroughness, institutional memory, and steady output, but may resist untested changes. They value clear expectations, documented processes, and demonstrable competence.";
            case ISFJ -> "ISFJs combine meticulous attention to detail with genuine care for people. They remember what matters to others, maintain team harmony, and handle essential tasks without seeking recognition. In teams, they contribute stability, practical support, and institutional knowledge. They are energised by helping others succeed and frustrated by carelessness or broken commitments.";
            case ESTJ -> "ESTJs bring order, structure, and clear expectations to any environment. They organise workflows, enforce standards, and ensure deadlines are met. In teams, they contribute decisive leadership and operational excellence, but may be perceived as rigid. They value tradition, clear authority, and measurable results.";
            case ESFJ -> "ESFJs create welcoming, well-organised environments where people feel valued. They are attuned to social dynamics, remember personal details, and actively maintain team cohesion. In teams, they contribute interpersonal warmth, practical organisation, and conflict resolution. They are energised by harmonious collaboration and troubled by discord or exclusion.";
            case ISTP -> "ISTPs understand how things work by taking them apart. They are calm under pressure, practical in crisis, and skilled at improvising solutions with available resources. In teams, they contribute technical competence and cool-headed troubleshooting, but may disengage from politics or lengthy meetings. They value autonomy and hands-on experience.";
            case ISFP -> "ISFPs bring aesthetic sensitivity and quiet authenticity to their work. They notice details others miss, create harmonious environments, and express values through action rather than words. In teams, they contribute creativity, flexibility, and genuine presence. They are energised by beauty and personal expression, and withdraw from environments that feel impersonal.";
            case ESTP -> "ESTPs are energised by doing. They read situations quickly, take calculated risks, and adapt in real time. In teams, they contribute decisive action, practical solutions, and an ability to cut through analysis paralysis. They can be impatient with theory or long planning cycles. They value direct experience and tangible results.";
            case ESFP -> "ESFPs bring warmth, humour, and positive energy to any setting. They are naturally attuned to the mood of a room and skilled at making others comfortable. In teams, they contribute enthusiasm, adaptability, and the ability to defuse tension. They learn by doing, value experiences over abstractions, and thrive in dynamic environments.";
        };
    }


}
