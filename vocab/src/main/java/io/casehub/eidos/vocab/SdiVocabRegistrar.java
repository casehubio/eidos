package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SdiVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<SdiTerm> vocabulary() {
        return SdiTerm.class;
    }
}
