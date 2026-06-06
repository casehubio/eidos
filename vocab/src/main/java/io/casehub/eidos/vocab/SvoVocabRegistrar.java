package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SvoVocabRegistrar implements VocabularyRegistrar {
    @Override public Class<SvoTerm> vocabulary() { return SvoTerm.class; }
}
