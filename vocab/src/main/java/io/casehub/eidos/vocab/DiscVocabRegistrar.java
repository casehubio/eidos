package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DiscVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<DiscTerm> vocabulary() {
        return DiscTerm.class;
    }
}
