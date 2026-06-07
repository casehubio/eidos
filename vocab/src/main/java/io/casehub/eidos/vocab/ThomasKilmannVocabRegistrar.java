package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ThomasKilmannVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<ThomasKilmannTerm> vocabulary() {
        return ThomasKilmannTerm.class;
    }
}
