package io.casehub.eidos.api;

import java.util.OptionalLong;

public final class BehavioralExpectations {

    private BehavioralExpectations() {}

    public static OptionalLong latencyBound(final AgentCapability capability) {
        return capability.latencyHintP50Ms() != null
                ? OptionalLong.of(capability.latencyHintP50Ms())
                : OptionalLong.empty();
    }

    public static boolean delegationExpected(final AgentDisposition disposition) {
        return disposition != null && disposition.delegation();
    }

    public static boolean escalationExpected(final AgentDisposition disposition,
                                             final String autonomyVocabUri,
                                             final VocabularyRegistry registry) {
        if (disposition == null || disposition.autonomy().isEmpty()) {return false;}
        if (autonomyVocabUri == null || registry == null) {return false;}
        final String autonomyValue = disposition.primaryTerm(DispositionAxis.AUTONOMY);
        if (autonomyValue == null) {return false;}
        return registry.resolve(autonomyVocabUri, autonomyValue)
                       .map(VocabularyTerm::impliesSupervision)
                       .orElse(false);}

    public static boolean escalationExpected(final AgentDescriptor descriptor,
                                             final VocabularyRegistry registry) {
        if (descriptor == null || descriptor.disposition() == null) return false;
        return descriptor.vocabUriForAxis(DispositionAxis.AUTONOMY)
                .map(uri -> escalationExpected(descriptor.disposition(), uri, registry))
                .orElse(false);
    }
}
