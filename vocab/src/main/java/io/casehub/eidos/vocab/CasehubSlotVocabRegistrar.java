package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CasehubSlotVocabRegistrar implements VocabularyRegistrar {
    @Override public Class<CasehubSlotTerm> vocabulary() { return CasehubSlotTerm.class; }
}
