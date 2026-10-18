package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.DataDrivenVocabulary;

import java.util.List;

@FunctionalInterface
public interface DataDrivenVocabularyRegistrar {
    List<DataDrivenVocabulary> vocabularies();
}
