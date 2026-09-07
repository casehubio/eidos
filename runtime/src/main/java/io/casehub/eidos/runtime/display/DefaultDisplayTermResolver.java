package io.casehub.eidos.runtime.display;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DisplayTermResolver;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@DefaultBean
@ApplicationScoped
public class DefaultDisplayTermResolver implements DisplayTermResolver {

    private final VocabularyRegistry registry;

    @Inject
    public DefaultDisplayTermResolver(VocabularyRegistry registry) {
        this.registry = registry;
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
}
