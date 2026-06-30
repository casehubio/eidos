package io.casehub.eidos.vocab;

import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CasehubCapabilityRegistrar implements VocabularyRegistrar {
    @Override public Class<CasehubCapabilityTerm> vocabulary() { return CasehubCapabilityTerm.class; }
}
