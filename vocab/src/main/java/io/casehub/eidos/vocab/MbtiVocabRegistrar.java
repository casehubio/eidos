package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MbtiVocabRegistrar implements VocabularyRegistrar {
    @Override
    public Class<? extends Enum<? extends VocabularyTerm>> vocabulary() {
        return MbtiTypeTerm.class;
    }
}
