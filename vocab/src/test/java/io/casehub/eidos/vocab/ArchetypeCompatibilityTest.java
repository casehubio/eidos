package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeCompatibilityTest {

    @Test
    void intjMapsToSageMagicianSovereign() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(MbtiTypeTerm.URI, "intj");
        assertTrue(families.contains(ArchetypeFamily.SAGE));
        assertTrue(families.contains(ArchetypeFamily.MAGICIAN));
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
        assertEquals(3, families.size());
    }

    @Test
    void entjMapsToSovereignHeroMagician() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(MbtiTypeTerm.URI, "entj");
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
        assertTrue(families.contains(ArchetypeFamily.HERO));
        assertTrue(families.contains(ArchetypeFamily.MAGICIAN));
    }

    @Test
    void enfpMapsToExplorerCreatorJester() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(MbtiTypeTerm.URI, "enfp");
        assertTrue(families.contains(ArchetypeFamily.EXPLORER));
        assertTrue(families.contains(ArchetypeFamily.CREATOR));
        assertTrue(families.contains(ArchetypeFamily.JESTER));
    }

    @Test
    void enneagram5MapsToSageExplorerMagician() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(EnneagramTerm.URI, "type-5");
        assertTrue(families.contains(ArchetypeFamily.SAGE));
        assertTrue(families.contains(ArchetypeFamily.EXPLORER));
        assertTrue(families.contains(ArchetypeFamily.MAGICIAN));
    }

    @Test
    void enneagram8MapsToRebelSovereignHero() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(EnneagramTerm.URI, "type-8");
        assertTrue(families.contains(ArchetypeFamily.REBEL));
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
        assertTrue(families.contains(ArchetypeFamily.HERO));
    }

    @Test
    void discDominanceMapsToMasteryGroup() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(DiscTerm.URI, "dominance");
        assertTrue(families.contains(ArchetypeFamily.HERO));
        assertTrue(families.contains(ArchetypeFamily.REBEL));
        assertTrue(families.contains(ArchetypeFamily.MAGICIAN));
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
    }

    @Test
    void discConscientiousnessMapsToIndependenceGroup() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(DiscTerm.URI, "conscientiousness-disc");
        assertTrue(families.contains(ArchetypeFamily.SAGE));
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
        assertTrue(families.contains(ArchetypeFamily.CREATOR));
        assertTrue(families.contains(ArchetypeFamily.EXPLORER));
    }

    @Test
    void belbinPlantMapsToCreatorMagician() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(BelbinTerm.URI, "plant");
        assertTrue(families.contains(ArchetypeFamily.CREATOR));
        assertTrue(families.contains(ArchetypeFamily.MAGICIAN));
    }

    @Test
    void belbinMonitorEvaluatorMapsToSageSovereign() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies(BelbinTerm.URI, "monitor-evaluator");
        assertTrue(families.contains(ArchetypeFamily.SAGE));
        assertTrue(families.contains(ArchetypeFamily.SOVEREIGN));
    }

    @Test
    void detectiveHasIntjAffinity() {
        assertTrue(ArchetypeCompatibility
            .subArchetypeAffinity(ArchetypeTerm.DETECTIVE, MbtiTypeTerm.URI, "intj"));
        assertTrue(ArchetypeCompatibility
            .subArchetypeAffinity(ArchetypeTerm.DETECTIVE, MbtiTypeTerm.URI, "istj"));
    }

    @Test
    void detectiveDoesNotHaveEnfpAffinity() {
        assertFalse(ArchetypeCompatibility
            .subArchetypeAffinity(ArchetypeTerm.DETECTIVE, MbtiTypeTerm.URI, "enfp"));
    }

    @Test
    void unknownFrameworkReturnsAllFamilies() {
        Set<ArchetypeFamily> families = ArchetypeCompatibility
            .compatibleFamilies("urn:casehub:vocab:unknown", "x");
        assertEquals(12, families.size());
    }

    @Test
    void allMbtiTypesCovered() {
        var mbtiValues = java.util.List.of(
            "intj", "intp", "entj", "entp", "infj", "infp", "enfj", "enfp",
            "istj", "isfj", "estj", "esfj", "istp", "isfp", "estp", "esfp");
        for (String type : mbtiValues) {
            var families = ArchetypeCompatibility
                .compatibleFamilies(MbtiTypeTerm.URI, type);
            assertFalse(families.isEmpty(), "MBTI " + type + " has no compatible families");
            assertTrue(families.size() <= 4,
                "MBTI " + type + " maps to too many families: " + families.size());
        }
    }

    @Test
    void allEnneagramTypesCovered() {
        for (int i = 1; i <= 9; i++) {
            var families = ArchetypeCompatibility
                .compatibleFamilies(EnneagramTerm.URI, "type-" + i);
            assertFalse(families.isEmpty(), "Enneagram " + i + " has no compatible families");
        }
    }
}
