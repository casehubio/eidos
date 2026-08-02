package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BigFiveVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<BigFiveTerm> vocabulary() {
        return BigFiveTerm.class;
    }
}
