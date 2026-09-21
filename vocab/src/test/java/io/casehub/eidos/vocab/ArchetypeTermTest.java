package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeTermTest {

    @Test
    void familyCountIs12() {
        assertEquals(12, ArchetypeFamily.values().length);
    }

    @Test
    void eachFamilyHasQuadrant() {
        for (ArchetypeFamily f : ArchetypeFamily.values()) {
            assertNotNull(f.quadrant(), f.name() + " missing quadrant");
            assertNotNull(f.drive(), f.name() + " missing drive");
            assertNotNull(f.label(), f.name() + " missing label");
        }
    }

    @Test
    void sageFamilyHasIndependenceQuadrant() {
        assertEquals(ArchetypeFamily.Quadrant.INDEPENDENCE, ArchetypeFamily.SAGE.quadrant());
    }

    @Test
    void heroFamilyHasMasteryQuadrant() {
        assertEquals(ArchetypeFamily.Quadrant.MASTERY, ArchetypeFamily.HERO.quadrant());
    }

    @Test
    void caregiverFamilyHasStabilityQuadrant() {
        assertEquals(ArchetypeFamily.Quadrant.STABILITY, ArchetypeFamily.CAREGIVER.quadrant());
    }

    @Test
    void everymanFamilyHasBelongingQuadrant() {
        assertEquals(ArchetypeFamily.Quadrant.BELONGING, ArchetypeFamily.EVERYMAN.quadrant());
    }

    @Test
    void termCountIs48() {
        assertEquals(48, ArchetypeTerm.values().length);
    }

    @Test
    void eachFamilyHas4SubArchetypes() {
        for (ArchetypeFamily f : ArchetypeFamily.values()) {
            long count = Arrays.stream(ArchetypeTerm.values())
                .filter(t -> t.family() == f)
                .count();
            assertEquals(4, count, f.name() + " should have 4 sub-archetypes, has " + count);
        }
    }

    @Test
    void detectiveBelongsToSageFamily() {
        assertEquals(ArchetypeFamily.SAGE, ArchetypeTerm.DETECTIVE.family());
        assertEquals("detective", ArchetypeTerm.DETECTIVE.value());
        assertEquals("Detective", ArchetypeTerm.DETECTIVE.label());
        assertFalse(ArchetypeTerm.DETECTIVE.description().isBlank());
    }

    @Test
    void eachTermHasValidAndInvalidAdjectives() {
        for (ArchetypeTerm t : ArchetypeTerm.values()) {
            assertFalse(t.validAdjectives().isEmpty(),
                t.name() + " has no valid adjectives");
            assertFalse(t.invalidAdjectives().isEmpty(),
                t.name() + " has no invalid adjectives");
        }
    }

    @Test
    void validAndInvalidAdjectivesDoNotOverlap() {
        for (ArchetypeTerm t : ArchetypeTerm.values()) {
            var overlap = new HashSet<>(t.validAdjectives());
            overlap.retainAll(t.invalidAdjectives());
            assertTrue(overlap.isEmpty(),
                t.name() + " has overlapping adjectives: " + overlap);
        }
    }

    @Test
    void vocabularyMetadataPresent() {
        var meta = ArchetypeTerm.class.getAnnotation(VocabularyMetadata.class);
        assertNotNull(meta);
        assertEquals("urn:casehub:vocab:archetype", meta.uri());
    }

    @Test
    void implementsVocabularyTerm() {
        assertTrue(VocabularyTerm.class.isAssignableFrom(ArchetypeTerm.class));
    }

    @Test
    void allValuesUnique() {
        var values = Arrays.stream(ArchetypeTerm.values())
            .map(ArchetypeTerm::value)
            .collect(Collectors.toSet());
        assertEquals(ArchetypeTerm.values().length, values.size(), "Duplicate values found");
    }

    @Test
    void byFamilyReturnsCorrectTerms() {
        var sageTerms = ArchetypeTerm.byFamily(ArchetypeFamily.SAGE);
        assertEquals(4, sageTerms.size());
        assertTrue(sageTerms.contains(ArchetypeTerm.DETECTIVE));
        assertTrue(sageTerms.contains(ArchetypeTerm.MENTOR));
        assertTrue(sageTerms.contains(ArchetypeTerm.SHAMAN));
        assertTrue(sageTerms.contains(ArchetypeTerm.TRANSLATOR));
    }
}
