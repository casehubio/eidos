package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeResolverTest {

    @Test
    void intjAndEnneagram5NarrowsToSageOrMagician() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "intj",
            EnneagramTerm.URI, "type-5"));
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        var narrowed = (ArchetypeResolver.Narrowed) result;
        assertTrue(narrowed.candidates().stream()
            .allMatch(t -> t.family() == ArchetypeFamily.SAGE
                        || t.family() == ArchetypeFamily.MAGICIAN));
    }

    @Test
    void intjEnneagram5DiscCConvergesToSageFamily() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "intj",
            EnneagramTerm.URI, "type-5",
            DiscTerm.URI, "conscientiousness-disc"));
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        var narrowed = (ArchetypeResolver.Narrowed) result;
        assertTrue(narrowed.candidates().stream()
            .allMatch(t -> t.family() == ArchetypeFamily.SAGE));
    }

    @Test
    void isfjAndEnneagram8Conflicts() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "isfj",
            EnneagramTerm.URI, "type-8"));
        assertInstanceOf(ArchetypeResolver.Conflict.class, result);
    }

    @Test
    void singleFrameworkNarrows() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "estp"));
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        var narrowed = (ArchetypeResolver.Narrowed) result;
        assertTrue(narrowed.candidates().size() < 48);
        assertTrue(narrowed.candidates().size() > 0);
    }

    @Test
    void emptyInputReturnsAll() {
        var result = ArchetypeResolver.resolve(Map.of());
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        assertEquals(48, ((ArchetypeResolver.Narrowed) result).candidates().size());
    }

    @Test
    void affinityRefinementNarrowsWithinFamily() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "istj",
            EnneagramTerm.URI, "type-5",
            DiscTerm.URI, "conscientiousness-disc"));
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        var narrowed = (ArchetypeResolver.Narrowed) result;
        assertTrue(narrowed.candidates().contains(ArchetypeTerm.DETECTIVE));
        assertTrue(narrowed.candidates().stream()
            .allMatch(t -> t.family() == ArchetypeFamily.SAGE),
            "All candidates should be in Sage family");
    }

    @Test
    void estpEnneagram7ConvergesToJesterFamily() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "estp",
            EnneagramTerm.URI, "type-7"));
        assertInstanceOf(ArchetypeResolver.Narrowed.class, result);
        var narrowed = (ArchetypeResolver.Narrowed) result;
        assertTrue(narrowed.candidates().stream()
            .allMatch(t -> t.family() == ArchetypeFamily.JESTER));
    }

    @Test
    void belbinPlantEntpConvergesToMagician() {
        var result = ArchetypeResolver.resolve(Map.of(
            BelbinTerm.URI, "plant",
            MbtiTypeTerm.URI, "entp"));
        switch (result) {
            case ArchetypeResolver.Converged c ->
                assertEquals(ArchetypeFamily.MAGICIAN, c.archetype().family());
            case ArchetypeResolver.Narrowed n ->
                assertTrue(n.candidates().stream()
                    .allMatch(t -> t.family() == ArchetypeFamily.MAGICIAN));
            case ArchetypeResolver.Conflict c ->
                fail("Should not conflict: " + c);
        }
    }

    @Test
    void conflictReportsTheConflictingPair() {
        var result = ArchetypeResolver.resolve(Map.of(
            MbtiTypeTerm.URI, "isfj",
            EnneagramTerm.URI, "type-8"));
        assertInstanceOf(ArchetypeResolver.Conflict.class, result);
        var conflict = (ArchetypeResolver.Conflict) result;
        assertNotNull(conflict.frameworkUriA());
        assertNotNull(conflict.frameworkUriB());
    }
}
