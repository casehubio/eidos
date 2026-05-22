package io.casehub.eidos.api;

import java.util.Optional;
import java.util.Set;

public interface VocabularyRegistry {
    void register(Vocabulary vocabulary);
    Optional<Vocabulary> find(String uri);
    Optional<VocabularyTerm> resolve(String vocabularyUri, String value);
    Set<String> equivalentValues(String vocabularyUri, String value, String targetVocabularyUri);
}
