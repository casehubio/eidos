package io.casehub.eidos.api;

import java.util.List;
import java.util.Map;

/**
 * A term within a Vocabulary. exactMatches maps to equivalent terms in other vocabularies,
 * enabling cross-vocabulary discovery without requiring query authors to know all vocabularies.
 */
public record VocabularyTerm(
        String value,
        String label,
        String description,
        List<String> aliases,
        Map<String, String> exactMatches
) {}
