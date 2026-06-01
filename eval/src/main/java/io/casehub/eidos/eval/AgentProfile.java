package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.GoalContext;

import java.util.List;
import java.util.Map;

public record AgentProfile(
    String name,
    String role,
    String domain,
    String sourceUrl,
    String sourceCitation,
    SourceType sourceType,
    String originalProse,
    GoalContext evalGoal,
    String notes,
    Map<String, String> theoreticalFramework,
    Map<String, TraitPolarity> expectedTraits,
    AgentDescriptor descriptor,
    List<VocabularyGap> vocabularyGaps
) {}
