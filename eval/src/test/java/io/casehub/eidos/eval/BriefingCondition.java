package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;

enum BriefingCondition {

    BASELINE_MINIMAL(false, false),
    BASELINE_RICH(false, true),
    JUNGIAN_MINIMAL(true, false),
    JUNGIAN_RICH(true, true);

    private final boolean keepFramework;
    private final boolean keepBriefing;

    BriefingCondition(boolean keepFramework, boolean keepBriefing) {
        this.keepFramework = keepFramework;
        this.keepBriefing = keepBriefing;
    }

    AgentDescriptor apply(JungianProfile profile) {
        final AgentDescriptor orig = profile.descriptor();
        final String briefing = keepBriefing
            ? orig.briefing()
            : "You are an agent named " + extractRole(profile.role());

        final AgentDisposition disposition = keepFramework
            ? orig.disposition()
            : AgentDisposition.builder()
                .delegation(orig.disposition() != null && orig.disposition().delegation())
                .build();

        final String dispositionVocabulary = keepFramework
            ? orig.dispositionVocabulary()
            : null;

        return AgentDescriptor.builder()
            .agentId(orig.agentId())
            .name(orig.name())
            .version(orig.version())
            .provider(orig.provider())
            .modelFamily(orig.modelFamily())
            .modelVersion(orig.modelVersion())
            .weightsFingerprint(orig.weightsFingerprint())
            .domainVocabulary(orig.domainVocabulary())
            .slotVocabulary(orig.slotVocabulary())
            .dispositionVocabulary(dispositionVocabulary)
            .axisVocabularies(keepFramework ? orig.axisVocabularies() : null)
            .slot(orig.slot())
            .capabilities(orig.capabilities())
            .disposition(disposition)
            .jurisdiction(orig.jurisdiction())
            .dataHandlingPolicy(orig.dataHandlingPolicy())
            .tenancyId(orig.tenancyId())
            .briefing(briefing)
            .templates(orig.templates())
            .goals(orig.goals())
            .constraints(orig.constraints())
            .build();
    }

    private static String extractRole(String role) {
        final int dash = role.indexOf(" — ");
        return dash > 0 ? role.substring(0, dash) : role;
    }
}
