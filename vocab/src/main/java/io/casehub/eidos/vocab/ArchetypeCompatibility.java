package io.casehub.eidos.vocab;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.casehub.eidos.vocab.ArchetypeFamily.*;

public final class ArchetypeCompatibility {

    private static final Map<String, Map<String, Set<ArchetypeFamily>>> FAMILY_MAP = new HashMap<>();
    private static final Map<ArchetypeTerm, Map<String, Set<String>>> AFFINITY_MAP = new HashMap<>();
    private static final Set<ArchetypeFamily> ALL_FAMILIES = EnumSet.allOf(ArchetypeFamily.class);

    static {
        initMbti();
        initEnneagram();
        initDisc();
        initBelbin();
        initBigFive();
        initSdi();
        initAffinities();
    }

    private ArchetypeCompatibility() {}

    public static Set<ArchetypeFamily> compatibleFamilies(String frameworkUri, String termValue) {
        var framework = FAMILY_MAP.get(frameworkUri);
        if (framework == null) return ALL_FAMILIES;
        var families = framework.get(termValue.toLowerCase(Locale.ROOT));
        return families != null ? families : ALL_FAMILIES;
    }

    public static boolean subArchetypeAffinity(ArchetypeTerm archetype,
                                                String frameworkUri, String termValue) {
        var affinities = AFFINITY_MAP.get(archetype);
        if (affinities == null) return false;
        var values = affinities.get(frameworkUri);
        return values != null && values.contains(termValue.toLowerCase(Locale.ROOT));
    }

    private static void initMbti() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("intj", Set.of(SAGE, MAGICIAN, SOVEREIGN));
        m.put("intp", Set.of(SAGE, EXPLORER, CREATOR));
        m.put("entj", Set.of(SOVEREIGN, HERO, MAGICIAN));
        m.put("entp", Set.of(MAGICIAN, REBEL, EXPLORER));
        m.put("infj", Set.of(MAGICIAN, SAGE, CAREGIVER));
        m.put("infp", Set.of(CREATOR, INNOCENT, EXPLORER));
        m.put("enfj", Set.of(CAREGIVER, SOVEREIGN, MAGICIAN));
        m.put("enfp", Set.of(EXPLORER, CREATOR, JESTER));
        m.put("istj", Set.of(SOVEREIGN, EVERYMAN, SAGE));
        m.put("isfj", Set.of(CAREGIVER, EVERYMAN, INNOCENT));
        m.put("estj", Set.of(SOVEREIGN, HERO, EVERYMAN));
        m.put("esfj", Set.of(CAREGIVER, EVERYMAN, LOVER));
        m.put("istp", Set.of(EXPLORER, HERO, REBEL));
        m.put("isfp", Set.of(CREATOR, LOVER, INNOCENT));
        m.put("estp", Set.of(HERO, JESTER, REBEL));
        m.put("esfp", Set.of(JESTER, LOVER, INNOCENT));
        FAMILY_MAP.put(MbtiTypeTerm.URI, m);
    }

    private static void initEnneagram() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("type-1", Set.of(HERO, SOVEREIGN, SAGE));
        m.put("type-2", Set.of(CAREGIVER, LOVER, EVERYMAN));
        m.put("type-3", Set.of(HERO, MAGICIAN, SOVEREIGN));
        m.put("type-4", Set.of(CREATOR, REBEL, LOVER));
        m.put("type-5", Set.of(SAGE, EXPLORER, MAGICIAN));
        m.put("type-6", Set.of(EVERYMAN, CAREGIVER, HERO));
        m.put("type-7", Set.of(JESTER, EXPLORER, INNOCENT));
        m.put("type-8", Set.of(REBEL, SOVEREIGN, HERO));
        m.put("type-9", Set.of(INNOCENT, EVERYMAN, CAREGIVER));
        FAMILY_MAP.put(EnneagramTerm.URI, m);
    }

    private static void initDisc() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("dominance", Set.of(HERO, REBEL, MAGICIAN, SOVEREIGN));
        m.put("influence", Set.of(JESTER, LOVER, EVERYMAN, EXPLORER));
        m.put("steadiness", Set.of(CAREGIVER, EVERYMAN, INNOCENT, CREATOR));
        m.put("conscientiousness-disc", Set.of(SAGE, SOVEREIGN, CREATOR, EXPLORER));
        FAMILY_MAP.put(DiscTerm.URI, m);
    }

    private static void initBelbin() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("plant", Set.of(CREATOR, MAGICIAN));
        m.put("resource-investigator", Set.of(EXPLORER, JESTER, EVERYMAN));
        m.put("co-ordinator", Set.of(SOVEREIGN, CAREGIVER));
        m.put("shaper", Set.of(HERO, REBEL, SOVEREIGN));
        m.put("monitor-evaluator", Set.of(SAGE, SOVEREIGN));
        m.put("teamworker", Set.of(EVERYMAN, CAREGIVER, LOVER));
        m.put("implementer", Set.of(EVERYMAN, SOVEREIGN));
        m.put("completer-finisher", Set.of(SAGE, SOVEREIGN));
        m.put("specialist", Set.of(SAGE, EXPLORER));
        FAMILY_MAP.put(BelbinTerm.URI, m);
    }

    private static void initBigFive() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("openness-high", Set.of(CREATOR, EXPLORER, MAGICIAN, REBEL));
        m.put("openness-low", Set.of(SOVEREIGN, EVERYMAN, CAREGIVER));
        m.put("conscientiousness-high", Set.of(SOVEREIGN, HERO, SAGE));
        m.put("conscientiousness-low", Set.of(JESTER, REBEL, EXPLORER));
        m.put("extraversion-high", Set.of(JESTER, HERO, LOVER, EVERYMAN));
        m.put("extraversion-low", Set.of(SAGE, CREATOR, INNOCENT));
        m.put("agreeableness-high", Set.of(CAREGIVER, EVERYMAN, INNOCENT, LOVER));
        m.put("agreeableness-low", Set.of(REBEL, SOVEREIGN, HERO));
        m.put("neuroticism-high", Set.of(CREATOR, REBEL, LOVER));
        m.put("neuroticism-low", Set.of(SAGE, SOVEREIGN, INNOCENT, EVERYMAN));
        FAMILY_MAP.put(BigFiveTerm.URI, m);
    }

    private static void initSdi() {
        var m = new HashMap<String, Set<ArchetypeFamily>>();
        m.put("blue", Set.of(CAREGIVER, INNOCENT, LOVER));
        m.put("red", Set.of(HERO, SOVEREIGN, REBEL));
        m.put("green", Set.of(SAGE, EXPLORER, CREATOR));
        m.put("hub", Set.of(EVERYMAN, MAGICIAN, JESTER));
        FAMILY_MAP.put(SdiTerm.URI, m);
    }

    private static void initAffinities() {
        // Caregiver family
        aff(ArchetypeTerm.ANGEL, MbtiTypeTerm.URI, "infj", "isfj");
        aff(ArchetypeTerm.ANGEL, EnneagramTerm.URI, "type-2", "type-9");
        aff(ArchetypeTerm.GUARDIAN, MbtiTypeTerm.URI, "istj", "isfj", "estj");
        aff(ArchetypeTerm.GUARDIAN, EnneagramTerm.URI, "type-6", "type-1");
        aff(ArchetypeTerm.HEALER, MbtiTypeTerm.URI, "infj", "infp", "isfp");
        aff(ArchetypeTerm.HEALER, EnneagramTerm.URI, "type-2", "type-4");
        aff(ArchetypeTerm.SAMARITAN, MbtiTypeTerm.URI, "esfj", "enfj", "isfj");
        aff(ArchetypeTerm.SAMARITAN, EnneagramTerm.URI, "type-2", "type-6");

        // Creator family
        aff(ArchetypeTerm.ARTIST, MbtiTypeTerm.URI, "isfp", "infp");
        aff(ArchetypeTerm.ARTIST, EnneagramTerm.URI, "type-4");
        aff(ArchetypeTerm.ENTREPRENEUR, MbtiTypeTerm.URI, "entp", "entj", "estp");
        aff(ArchetypeTerm.ENTREPRENEUR, EnneagramTerm.URI, "type-3", "type-7");
        aff(ArchetypeTerm.STORYTELLER, MbtiTypeTerm.URI, "enfp", "infp", "enfj");
        aff(ArchetypeTerm.STORYTELLER, EnneagramTerm.URI, "type-4", "type-7");
        aff(ArchetypeTerm.VISIONARY, MbtiTypeTerm.URI, "intj", "infj", "entp");
        aff(ArchetypeTerm.VISIONARY, EnneagramTerm.URI, "type-5", "type-4");

        // Everyman family
        aff(ArchetypeTerm.ADVOCATE, MbtiTypeTerm.URI, "enfj", "infj", "enfp");
        aff(ArchetypeTerm.ADVOCATE, EnneagramTerm.URI, "type-1", "type-6");
        aff(ArchetypeTerm.NETWORKER, MbtiTypeTerm.URI, "enfp", "esfj", "entp");
        aff(ArchetypeTerm.NETWORKER, EnneagramTerm.URI, "type-7", "type-3");
        aff(ArchetypeTerm.SERVANT, MbtiTypeTerm.URI, "isfj", "istj");
        aff(ArchetypeTerm.SERVANT, EnneagramTerm.URI, "type-2", "type-9");
        aff(ArchetypeTerm.CITIZEN, MbtiTypeTerm.URI, "estj", "istj", "esfj");
        aff(ArchetypeTerm.CITIZEN, EnneagramTerm.URI, "type-6", "type-1");

        // Explorer family
        aff(ArchetypeTerm.ADVENTURER, MbtiTypeTerm.URI, "estp", "istp", "esfp");
        aff(ArchetypeTerm.ADVENTURER, EnneagramTerm.URI, "type-7", "type-8");
        aff(ArchetypeTerm.GENERALIST, MbtiTypeTerm.URI, "entp", "enfp", "intp");
        aff(ArchetypeTerm.GENERALIST, EnneagramTerm.URI, "type-7", "type-5");
        aff(ArchetypeTerm.PIONEER, MbtiTypeTerm.URI, "entj", "entp", "intj");
        aff(ArchetypeTerm.PIONEER, EnneagramTerm.URI, "type-3", "type-7", "type-8");
        aff(ArchetypeTerm.SEEKER, MbtiTypeTerm.URI, "infp", "infj", "intp");
        aff(ArchetypeTerm.SEEKER, EnneagramTerm.URI, "type-5", "type-4");

        // Hero family
        aff(ArchetypeTerm.ATHLETE, MbtiTypeTerm.URI, "estp", "istp", "estj");
        aff(ArchetypeTerm.ATHLETE, EnneagramTerm.URI, "type-3", "type-1");
        aff(ArchetypeTerm.LIBERATOR, MbtiTypeTerm.URI, "enfj", "entj", "enfp");
        aff(ArchetypeTerm.LIBERATOR, EnneagramTerm.URI, "type-8", "type-1");
        aff(ArchetypeTerm.RESCUER, MbtiTypeTerm.URI, "esfj", "isfj", "estj");
        aff(ArchetypeTerm.RESCUER, EnneagramTerm.URI, "type-2", "type-6");
        aff(ArchetypeTerm.WARRIOR, MbtiTypeTerm.URI, "entj", "estj", "intj");
        aff(ArchetypeTerm.WARRIOR, EnneagramTerm.URI, "type-8", "type-3", "type-1");

        // Innocent family
        aff(ArchetypeTerm.CHILD, MbtiTypeTerm.URI, "esfp", "enfp", "isfp");
        aff(ArchetypeTerm.CHILD, EnneagramTerm.URI, "type-7", "type-9");
        aff(ArchetypeTerm.DREAMER, MbtiTypeTerm.URI, "infp", "infj");
        aff(ArchetypeTerm.DREAMER, EnneagramTerm.URI, "type-9", "type-4");
        aff(ArchetypeTerm.IDEALIST, MbtiTypeTerm.URI, "infp", "enfj", "enfp");
        aff(ArchetypeTerm.IDEALIST, EnneagramTerm.URI, "type-1", "type-9");
        aff(ArchetypeTerm.MUSE, MbtiTypeTerm.URI, "enfp", "infp", "enfj");
        aff(ArchetypeTerm.MUSE, EnneagramTerm.URI, "type-4", "type-7");

        // Jester family
        aff(ArchetypeTerm.CLOWN, MbtiTypeTerm.URI, "esfp", "estp");
        aff(ArchetypeTerm.CLOWN, EnneagramTerm.URI, "type-7", "type-9");
        aff(ArchetypeTerm.ENTERTAINER, MbtiTypeTerm.URI, "esfp", "enfp", "estp");
        aff(ArchetypeTerm.ENTERTAINER, EnneagramTerm.URI, "type-7", "type-3");
        aff(ArchetypeTerm.PROVOCATEUR, MbtiTypeTerm.URI, "entp", "estp", "intj");
        aff(ArchetypeTerm.PROVOCATEUR, EnneagramTerm.URI, "type-7", "type-8", "type-4");
        aff(ArchetypeTerm.SHAPESHIFTER, MbtiTypeTerm.URI, "enfp", "entp", "infj");
        aff(ArchetypeTerm.SHAPESHIFTER, EnneagramTerm.URI, "type-3", "type-7", "type-9");

        // Lover family
        aff(ArchetypeTerm.COMPANION, MbtiTypeTerm.URI, "isfj", "isfp", "esfj");
        aff(ArchetypeTerm.COMPANION, EnneagramTerm.URI, "type-2", "type-6", "type-9");
        aff(ArchetypeTerm.HEDONIST, MbtiTypeTerm.URI, "esfp", "isfp", "estp");
        aff(ArchetypeTerm.HEDONIST, EnneagramTerm.URI, "type-7", "type-4");
        aff(ArchetypeTerm.MATCHMAKER, MbtiTypeTerm.URI, "enfj", "esfj", "enfp");
        aff(ArchetypeTerm.MATCHMAKER, EnneagramTerm.URI, "type-2", "type-7");
        aff(ArchetypeTerm.ROMANTIC, MbtiTypeTerm.URI, "infp", "enfp", "infj");
        aff(ArchetypeTerm.ROMANTIC, EnneagramTerm.URI, "type-4", "type-2");

        // Magician family
        aff(ArchetypeTerm.ALCHEMIST, MbtiTypeTerm.URI, "infj", "intj");
        aff(ArchetypeTerm.ALCHEMIST, EnneagramTerm.URI, "type-5", "type-4");
        aff(ArchetypeTerm.ENGINEER, MbtiTypeTerm.URI, "intj", "intp", "entj");
        aff(ArchetypeTerm.ENGINEER, EnneagramTerm.URI, "type-5", "type-1", "type-3");
        aff(ArchetypeTerm.INNOVATOR, MbtiTypeTerm.URI, "entp", "entj", "enfp");
        aff(ArchetypeTerm.INNOVATOR, EnneagramTerm.URI, "type-7", "type-3");
        aff(ArchetypeTerm.SCIENTIST, MbtiTypeTerm.URI, "intj", "intp", "istj");
        aff(ArchetypeTerm.SCIENTIST, EnneagramTerm.URI, "type-5", "type-1");

        // Rebel family
        aff(ArchetypeTerm.ACTIVIST, MbtiTypeTerm.URI, "enfj", "enfp", "entj");
        aff(ArchetypeTerm.ACTIVIST, EnneagramTerm.URI, "type-1", "type-8");
        aff(ArchetypeTerm.GAMBLER, MbtiTypeTerm.URI, "estp", "entp");
        aff(ArchetypeTerm.GAMBLER, EnneagramTerm.URI, "type-7", "type-8");
        aff(ArchetypeTerm.MAVERICK, MbtiTypeTerm.URI, "istp", "intp", "intj", "entp");
        aff(ArchetypeTerm.MAVERICK, EnneagramTerm.URI, "type-5", "type-8", "type-4");
        aff(ArchetypeTerm.REFORMER, MbtiTypeTerm.URI, "intj", "entj", "infj");
        aff(ArchetypeTerm.REFORMER, EnneagramTerm.URI, "type-1", "type-8");

        // Sage family
        aff(ArchetypeTerm.DETECTIVE, MbtiTypeTerm.URI, "istj", "intj", "istp");
        aff(ArchetypeTerm.DETECTIVE, EnneagramTerm.URI, "type-5", "type-6");
        aff(ArchetypeTerm.MENTOR, MbtiTypeTerm.URI, "enfj", "infj", "entj");
        aff(ArchetypeTerm.MENTOR, EnneagramTerm.URI, "type-1", "type-2", "type-5");
        aff(ArchetypeTerm.SHAMAN, MbtiTypeTerm.URI, "infj", "infp", "intp");
        aff(ArchetypeTerm.SHAMAN, EnneagramTerm.URI, "type-5", "type-4", "type-9");
        aff(ArchetypeTerm.TRANSLATOR, MbtiTypeTerm.URI, "intp", "entp", "intj");
        aff(ArchetypeTerm.TRANSLATOR, EnneagramTerm.URI, "type-5", "type-7");

        // Sovereign family
        aff(ArchetypeTerm.AMBASSADOR, MbtiTypeTerm.URI, "enfj", "esfj", "entj");
        aff(ArchetypeTerm.AMBASSADOR, EnneagramTerm.URI, "type-3", "type-2", "type-9");
        aff(ArchetypeTerm.JUDGE, MbtiTypeTerm.URI, "intj", "istj", "estj");
        aff(ArchetypeTerm.JUDGE, EnneagramTerm.URI, "type-1", "type-5", "type-6");
        aff(ArchetypeTerm.PATRIARCH, MbtiTypeTerm.URI, "estj", "entj", "istj");
        aff(ArchetypeTerm.PATRIARCH, EnneagramTerm.URI, "type-8", "type-1", "type-6");
        aff(ArchetypeTerm.RULER, MbtiTypeTerm.URI, "entj", "estj");
        aff(ArchetypeTerm.RULER, EnneagramTerm.URI, "type-8", "type-3", "type-1");
    }

    private static void aff(ArchetypeTerm term, String uri, String... values) {
        AFFINITY_MAP.computeIfAbsent(term, k -> new HashMap<>())
            .put(uri, Set.of(values));
    }
}
