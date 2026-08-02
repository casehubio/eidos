package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnneagramVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<EnneagramTerm> vocabulary() {
        return EnneagramTerm.class;
    }
}
