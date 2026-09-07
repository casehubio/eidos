package io.casehub.eidos.api;

public interface DisplayTermResolver {

    String resolveLabel(String value, String sourceVocabUri,
                        String targetVocabUri, DispositionAxis axis);

    default String resolveLabel(String value, String sourceVocabUri,
                                String targetVocabUri) {
        return resolveLabel(value, sourceVocabUri, targetVocabUri, null);
    }

    default String resolveLabel(String value, String sourceVocabUri) {
        return resolveLabel(value, sourceVocabUri, null, null);
    }
}
