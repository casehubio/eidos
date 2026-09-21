package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.vocab.ArchetypeResolver;
import io.casehub.eidos.vocab.AvatarCodec;

import java.util.Comparator;
import java.util.Map;

public final class ArchetypeDeriver {

    private ArchetypeDeriver() {}

    public static AgentDescriptor deriveArchetype(AgentDescriptor descriptor) {
        if (descriptor.archetype() != null) return deriveAvatar(descriptor);
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
            descriptor = descriptor.toBuilder().archetype(c.archetype().value()).build();
        } else if (result instanceof ArchetypeResolver.Narrowed n && !n.candidates().isEmpty()) {
            descriptor = descriptor.toBuilder().archetype(n.candidates().getFirst().value()).build();
        }

        return deriveAvatar(descriptor);
    }

    private static AgentDescriptor deriveAvatar(AgentDescriptor descriptor) {
        if (descriptor.archetype() == null || descriptor.avatar() != null) return descriptor;
        String code = AvatarCodec.defaultCode(AvatarCodec.DEFAULT_COLLECTION, descriptor.archetype());
        if (code == null) return descriptor;
        return descriptor.toBuilder().avatar(code).build();
    }
}
