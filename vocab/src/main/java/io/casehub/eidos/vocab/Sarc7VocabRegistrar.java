package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Sarc7VocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<Sarc7Term> vocabulary() {
        return Sarc7Term.class;
    }
}
