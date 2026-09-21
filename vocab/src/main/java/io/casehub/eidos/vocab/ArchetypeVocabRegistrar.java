package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArchetypeVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<ArchetypeTerm> vocabulary() {
        return ArchetypeTerm.class;
    }
}
