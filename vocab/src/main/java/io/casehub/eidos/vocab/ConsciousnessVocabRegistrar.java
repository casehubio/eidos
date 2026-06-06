package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConsciousnessVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<ConscientiousnessTerm> vocabulary() {
        return ConscientiousnessTerm.class;
    }
}
