package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.vocab.ArchetypeResolver;

import java.util.Comparator;
import java.util.Map;

public final class ArchetypeDeriver {

    private ArchetypeDeriver() {}

    public static AgentDescriptor deriveArchetype(AgentDescriptor descriptor) {
        if (descriptor.archetype() != null) return descriptor;
        if (descriptor.disposition() == null) return descriptor;
        var profile = descriptor.disposition().dispositionProfile();
        if (profile == null || profile.isEmpty()) return descriptor;

        String vocabUri = descriptor.dispositionVocabulary();
        if (vocabUri == null || vocabUri.isBlank()) {
            vocabUri = descriptor.domainVocabulary();
        }
        if (vocabUri == null || vocabUri.isBlank()) return descriptor;

        var primaryTerm = profile.stream()
            .max(Comparator.comparingDouble(DispositionValue::weight))
            .map(DispositionValue::term)
            .orElse(null);
        if (primaryTerm == null) return descriptor;

        var result = ArchetypeResolver.resolve(Map.of(vocabUri, primaryTerm));

        if (result instanceof ArchetypeResolver.Converged c) {
            return descriptor.toBuilder().archetype(c.archetype().value()).build();
        }
        if (result instanceof ArchetypeResolver.Narrowed n && !n.candidates().isEmpty()) {
            return descriptor.toBuilder().archetype(n.candidates().getFirst().value()).build();
        }
        return descriptor;
    }
}
