package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ModelTierVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<ModelTierTerm> vocabulary() {
        return ModelTierTerm.class;
    }
}
