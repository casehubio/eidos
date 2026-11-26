package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmpdFacetsTest {

    @Test
    void canonical_contains25Facets() {
        assertEquals(25, AmpdFacets.CANONICAL.size());
    }

    @Test
    void canonical_containsOriginal13() {
        assertTrue(AmpdFacets.CANONICAL.contains("Anxiousness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Callousness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Deceitfulness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Emotional Lability"));
        assertTrue(AmpdFacets.CANONICAL.contains("Grandiosity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Hostility"));
        assertTrue(AmpdFacets.CANONICAL.contains("Impulsivity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Intimacy Avoidance"));
        assertTrue(AmpdFacets.CANONICAL.contains("Manipulativeness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Risk Taking"));
        assertTrue(AmpdFacets.CANONICAL.contains("Suspiciousness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Withdrawal"));
        assertTrue(AmpdFacets.CANONICAL.contains("Attention Seeking"));
    }

    @Test
    void canonical_containsNew12() {
        assertTrue(AmpdFacets.CANONICAL.contains("Anhedonia"));
        assertTrue(AmpdFacets.CANONICAL.contains("Depressivity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Perseveration"));
        assertTrue(AmpdFacets.CANONICAL.contains("Rigid Perfectionism"));
        assertTrue(AmpdFacets.CANONICAL.contains("Distractibility"));
        assertTrue(AmpdFacets.CANONICAL.contains("Separation Insecurity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Restricted Affectivity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Irresponsibility"));
        assertTrue(AmpdFacets.CANONICAL.contains("Cognitive Perceptual Dysregulation"));
        assertTrue(AmpdFacets.CANONICAL.contains("Eccentricity"));
        assertTrue(AmpdFacets.CANONICAL.contains("Submissiveness"));
        assertTrue(AmpdFacets.CANONICAL.contains("Unusual Beliefs"));
    }

    @Test
    void canonicalise_newFacets() {
        assertEquals("Anhedonia", AmpdFacets.canonicalise("anhedonia"));
        assertEquals("Rigid Perfectionism", AmpdFacets.canonicalise("rigid perfectionism"));
        assertEquals("Cognitive Perceptual Dysregulation",
                AmpdFacets.canonicalise("cognitive perceptual dysregulation"));
        assertEquals("Separation Insecurity", AmpdFacets.canonicalise("SEPARATION INSECURITY"));
        assertEquals("Unusual Beliefs", AmpdFacets.canonicalise("  unusual beliefs  "));
    }

    @Test
    void canonicalise_rejectsUnknown() {
        assertThrows(IllegalArgumentException.class,
                () -> AmpdFacets.canonicalise("Not A Real Facet"));
    }
}
