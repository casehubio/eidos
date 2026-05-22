package io.casehub.eidos.api;

import java.util.Map;

/**
 * A named, versioned set of terms for one or more AgentDescriptor fields.
 * Apps register vocabularies via VocabularyRegistry. Discovery resolves
 * equivalences across vocabularies for cross-vocabulary queries.
 */
public record Vocabulary(
        String uri,
        String name,
        String version,
        Map<String, VocabularyTerm> terms
) {}
