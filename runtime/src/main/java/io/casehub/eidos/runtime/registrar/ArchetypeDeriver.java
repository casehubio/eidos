package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.PersonalityInput;
import io.casehub.eidos.api.PersonalityTypeDeriver;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.vocab.ArchetypeCompatibility;
import io.casehub.eidos.vocab.ArchetypeFamily;
import io.casehub.eidos.vocab.ArchetypeResolver;
import io.casehub.eidos.vocab.ArchetypeTerm;
import io.casehub.eidos.vocab.AvatarCodec;

import java.util.Comparator;
import java.util.Locale;
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

    public static AgentDescriptor deriveDispositionFromArchetype(AgentDescriptor descriptor,
                                                                  VocabularyRegistry vocabRegistry) {
        if (descriptor.archetype() == null) return descriptor;
        if (descriptor.disposition() != null) {
            var disp = descriptor.disposition();
            if (disp.dispositionProfile() != null && !disp.dispositionProfile().isEmpty()) return descriptor;
            if (disp.mbtiType() != null && !disp.mbtiType().isEmpty()) return descriptor;
            if (disp.enneagramType() != null && !disp.enneagramType().isEmpty()) return descriptor;
            for (var axis : io.casehub.eidos.api.DispositionAxis.values()) {
                if (!disp.get(axis).isEmpty()) return descriptor;
            }
        }

        String firstMbti = null;

        try {
            var term = ArchetypeTerm.valueOf(descriptor.archetype().toUpperCase(Locale.ROOT));
            var affinities = ArchetypeCompatibility.mbtiAffinities(term);
            if (!affinities.isEmpty()) firstMbti = affinities.getFirst();
        } catch (IllegalArgumentException ignored) {}

        if (firstMbti == null) {
            try {
                var family = ArchetypeFamily.valueOf(descriptor.archetype().toUpperCase(Locale.ROOT));
                firstMbti = ArchetypeCompatibility.firstMbtiForFamily(family);
            } catch (IllegalArgumentException ignored) {}
        }

        if (firstMbti == null) return descriptor;
        var input = new PersonalityInput(firstMbti, null, false, Map.of());
        var builder = AgentDisposition.builder();
        PersonalityTypeDeriver.derive(input, vocabRegistry, builder);
        var derived = builder.build();

        if (derived.dispositionProfile() == null || derived.dispositionProfile().isEmpty()) return descriptor;

        return descriptor.toBuilder().disposition(derived).build();
    }

    private static AgentDescriptor deriveAvatar(AgentDescriptor descriptor) {
        if (descriptor.archetype() == null || descriptor.avatar() != null) return descriptor;
        String code = AvatarCodec.defaultCode(AvatarCodec.DEFAULT_COLLECTION, descriptor.archetype());
        if (code == null) return descriptor;
        return descriptor.toBuilder().avatar(code).build();
    }
}
