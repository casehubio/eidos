package io.casehub.eidos.core.display;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.Optional;

public class DefaultDisplayTermResolver implements DisplayTermResolver {

    private final VocabularyRegistry registry;

    public DefaultDisplayTermResolver(VocabularyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String resolveLabel(String value, String vocabUri) {
        return resolveLabel(value, vocabUri, null, null);
    }

    @Override
    public String resolveLabel(String value, String sourceVocabUri,
                               String targetVocabUri, DispositionAxis axis) {
        if (value == null) return null;

        String resolvedSourceUri = sourceVocabUri;
        VocabularyTerm sourceTerm = null;

        if (sourceVocabUri != null) {
            sourceTerm = registry.resolve(sourceVocabUri, value).orElse(null);
        } else {
            for (String uri : registry.registeredUris()) {
                var term = registry.resolve(uri, value);
                if (term.isPresent()) {
                    sourceTerm = term.get();
                    resolvedSourceUri = uri;
                    break;
                }
            }
        }

        if (sourceTerm == null) return value;

        if (targetVocabUri == null || targetVocabUri.equals(resolvedSourceUri)) {
            return sourceTerm.label();
        }

        var targetValue = axis != null
            ? registry.equivalentValues(resolvedSourceUri, value, targetVocabUri, axis)
            : registry.equivalentValues(resolvedSourceUri, value, targetVocabUri);

        if (targetValue.isPresent()) {
            var targetTerm = registry.resolve(targetVocabUri, targetValue.get());
            if (targetTerm.isPresent()) {
                return targetTerm.get().label();
            }
            return targetValue.get();
        }

        return sourceTerm.label();
    }

    @Override
    public Optional<String> mapTerm(String value, String sourceVocabUri,
                                     String targetVocabUri) {
        return mapTerm(value, sourceVocabUri, targetVocabUri, null);
    }

    @Override
    public Optional<String> mapTerm(String value, String sourceVocabUri,
                                     String targetVocabUri, String mappingContext) {
        if (value == null || sourceVocabUri == null || targetVocabUri == null) {
            return Optional.empty();
        }
        DispositionAxis axis = parseAxis(mappingContext);
        return axis != null
            ? registry.equivalentValues(sourceVocabUri, value, targetVocabUri, axis)
            : registry.equivalentValues(sourceVocabUri, value, targetVocabUri);
    }

    private static DispositionAxis parseAxis(String mappingContext) {
        if (mappingContext == null) return null;
        for (DispositionAxis axis : DispositionAxis.values()) {
            if (axis.jsonKey().equals(mappingContext)) return axis;
        }
        return null;
    }
}
