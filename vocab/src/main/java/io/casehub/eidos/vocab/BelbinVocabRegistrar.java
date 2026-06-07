package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BelbinVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<BelbinTerm> vocabulary() {
        return BelbinTerm.class;
    }
}
