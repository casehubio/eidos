package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.VocabularyTerm;

/**
 * CDI SPI for vocabulary registration. Implement as an {@code @ApplicationScoped} bean
 * to auto-register a vocabulary enum with {@link io.casehub.eidos.api.VocabularyRegistry}
 * at startup. The enum class must carry {@link io.casehub.eidos.api.VocabularyMetadata}.
 */
@FunctionalInterface
public interface VocabularyRegistrar {
    Class<? extends Enum<? extends VocabularyTerm>> vocabulary();
}
