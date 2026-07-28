package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDescriptorComparatorTest {

    private static final AgentDisposition DISPOSITION = AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("principled")
            .riskAppetite("measured").autonomy("semi-autonomous")
            .conflictMode("compromising").delegation(false)
            .build();

    private static AgentDescriptor base() {
        return AgentDescriptor.builder()
                .agentId("agent-1").name("Alice").version("1.0")
                .provider("anthropic").modelFamily("claude").modelVersion("4.6")
                .weightsFingerprint("fp-abc")
                .domainVocabulary("urn:svo").slotVocabulary("urn:slot")
                .dispositionVocabulary("urn:disp")
                .axisVocabularies(Map.of(DispositionAxis.SOCIAL_ORIENTATION, "urn:svo"))
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .qualityHint(0.85)
                                .latencyHintP50Ms(2000L)
                                .costHint("medium")
                                .inputTypes(List.of("text/plain"))
                                .outputTypes(List.of("text/markdown"))
                                .tags(List.of("review"))
                                .epistemicDomains(Map.of("java", 0.95))
                                .excludedDomains(Set.of("cobol"))
                                .build()))
                .disposition(DISPOSITION)
                .jurisdiction("US")
                .dataHandlingPolicy("standard")
                .tenancyId("tenant-1")
                .briefing("Expert code reviewer")
                .build();
    }

    private static AgentDescriptor withField(java.util.function.UnaryOperator<AgentDescriptor.Builder> mutator) {
        var b = AgentDescriptor.builder()
                .agentId("agent-1").name("Alice").version("1.0")
                .provider("anthropic").modelFamily("claude").modelVersion("4.6")
                .weightsFingerprint("fp-abc")
                .domainVocabulary("urn:svo").slotVocabulary("urn:slot")
                .dispositionVocabulary("urn:disp")
                .axisVocabularies(Map.of(DispositionAxis.SOCIAL_ORIENTATION, "urn:svo"))
                .slot("reviewer")
                .capabilities(List.of(
                        AgentCapability.builder()
                                .name("code-review")
                                .qualityHint(0.85)
                                .latencyHintP50Ms(2000L)
                                .costHint("medium")
                                .inputTypes(List.of("text/plain"))
                                .outputTypes(List.of("text/markdown"))
                                .tags(List.of("review"))
                                .epistemicDomains(Map.of("java", 0.95))
                                .excludedDomains(Set.of("cobol"))
                                .build()))
                .disposition(DISPOSITION)
                .jurisdiction("US")
                .dataHandlingPolicy("standard")
                .tenancyId("tenant-1")
                .briefing("Expert code reviewer");
        return mutator.apply(b).build();
    }

    // --- Structural sync tests ---

    @Test
    void comparatorCoversAllDescriptorComponents() {
        int total = AgentDescriptor.class.getRecordComponents().length;
        int skipped = 2; // agentId, tenancyId
        assertThat(AgentDescriptorComparator.COMPARED_FIELD_COUNT).isEqualTo(total - skipped);
    }

    @Test
    void comparatorCoversAllCapabilityComponents() {
        int total = AgentCapability.class.getRecordComponents().length;
        int matchKey = 1; // name
        assertThat(AgentDescriptorComparator.COMPARED_CAPABILITY_FIELD_COUNT).isEqualTo(total - matchKey);
    }

    @Test
    void comparatorCoversAllDispositionComponents() {
        assertThat(AgentDescriptorComparator.COMPARED_DISPOSITION_FIELD_COUNT)
                .isEqualTo(AgentDisposition.class.getRecordComponents().length);
    }

    // --- Identical descriptors ---

    @Test
    void identicalDescriptors_matches() {
        var result = AgentDescriptorComparator.compare(base(), base());
        assertThat(result.matches()).isTrue();
        assertThat(result.drifts()).isEmpty();
    }

    @Test
    void identicalDescriptors_differentIdentityKeys_matches() {
        var desired = base();
        var actual = withField(b -> b.agentId("agent-2").tenancyId("tenant-2"));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.matches()).isTrue();
    }

    // --- Simple field drift ---

    @Test
    void nameDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.name("Bob")));
        assertThat(result.matches()).isFalse();
        assertThat(result.drifts()).hasSize(1);
        assertThat(result.drifts().get(0).field()).isEqualTo("name");
        assertThat(result.drifts().get(0).desiredValue()).isEqualTo("Alice");
        assertThat(result.drifts().get(0).actualValue()).isEqualTo("Bob");
    }

    @Test
    void slotDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.slot("planner")));
        assertThat(result.matches()).isFalse();
        assertThat(result.drifts()).extracting("field").containsExactly("slot");
    }

    @Test
    void versionDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.version("2.0")));
        assertThat(result.drifts()).extracting("field").containsExactly("version");
    }

    @Test
    void providerDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.provider("openai")));
        assertThat(result.drifts()).extracting("field").containsExactly("provider");
    }

    @Test
    void vocabularyFieldsDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.domainVocabulary("urn:other")));
        assertThat(result.drifts()).extracting("field").containsExactly("domainVocabulary");
    }

    @Test
    void briefingDrifted() {
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.briefing("Changed briefing")));
        assertThat(result.drifts()).extracting("field").containsExactly("briefing");
    }

    @Test
    void nullToNonNull_drifted() {
        var desired = withField(b -> b.weightsFingerprint(null));
        var actual = base();
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).extracting("field").containsExactly("weightsFingerprint");
    }

    @Test
    void nonNullToNull_drifted() {
        var desired = base();
        var actual = withField(b -> b.weightsFingerprint(null));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).extracting("field").containsExactly("weightsFingerprint");
    }

    @Test
    void axisVocabularies_entryDrifted() {
        var actual = withField(b -> b.axisVocabularies(
                Map.of(DispositionAxis.SOCIAL_ORIENTATION, "urn:other")));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactly("axisVocabularies[SOCIAL_ORIENTATION]");
    }

    @Test
    void axisVocabularies_entryAdded() {
        var actual = withField(b -> b.axisVocabularies(Map.of(
                DispositionAxis.SOCIAL_ORIENTATION, "urn:svo",
                DispositionAxis.AUTONOMY, "urn:new")));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactly("axisVocabularies[AUTONOMY]");
    }

    // --- Disposition drift ---

    @Test
    void disposition_axisDrifted() {
        var drifted = AgentDisposition.builder()
                                      .socialOrient("independent").ruleFollowing("principled")
                                      .riskAppetite("measured").autonomy("semi-autonomous")
                                      .conflictMode("compromising").delegation(false)
                                      .build();
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.disposition(drifted)));
        assertThat(result.drifts()).extracting("field").containsExactly("disposition.socialOrient");
        assertThat(result.drifts().get(0).desiredValue()).contains("collaborative");
        assertThat(result.drifts().get(0).actualValue()).contains("independent");
    }

    @Test
    void disposition_delegationDrifted() {
        var drifted = AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .conflictMode("compromising").delegation(true)
                .build();
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.disposition(drifted)));
        assertThat(result.drifts()).extracting("field").containsExactly("disposition.delegation");
    }

    @Test
    void disposition_nullDesired_nullActual_matches() {
        var desired = withField(b -> b.disposition(null));
        var actual = withField(b -> b.disposition(null));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).filteredOn(d -> d.field().startsWith("disposition")).isEmpty();
    }

    @Test
    void disposition_nullDesired_nonNullActual_drifted() {
        var desired = withField(b -> b.disposition(null));
        var result = AgentDescriptorComparator.compare(desired, base());
        assertThat(result.drifts()).extracting("field").contains("disposition");
    }

    @Test
    void disposition_multipleAxesDrifted() {
        var drifted = AgentDisposition.builder()
                .socialOrient("independent").ruleFollowing("flexible")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .conflictMode("compromising").delegation(false)
                .build();
        var result = AgentDescriptorComparator.compare(base(), withField(b -> b.disposition(drifted)));
        assertThat(result.drifts()).extracting("field")
                .containsExactlyInAnyOrder("disposition.socialOrient", "disposition.ruleFollowing");
    }

    // --- Capability drift ---

    @Test
    void capability_added() {
        var extra = AgentCapability.builder().name("planning").build();
        var actual = withField(b -> b.capabilities(List.of(
                AgentCapability.builder()
                        .name("code-review").qualityHint(0.85).latencyHintP50Ms(2000L)
                        .costHint("medium").inputTypes(List.of("text/plain"))
                        .outputTypes(List.of("text/markdown")).tags(List.of("review"))
                        .epistemicDomains(Map.of("java", 0.95)).excludedDomains(Set.of("cobol"))
                        .build(),
                extra)));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field").containsExactly("capabilities[planning]");
        assertThat(result.drifts().get(0).desiredValue()).isEqualTo("(absent)");
        assertThat(result.drifts().get(0).actualValue()).isEqualTo("(present)");
    }

    @Test
    void capability_removed() {
        var actual = withField(b -> b.capabilities(List.of()));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field").containsExactly("capabilities[code-review]");
        assertThat(result.drifts().get(0).desiredValue()).isEqualTo("(present)");
        assertThat(result.drifts().get(0).actualValue()).isEqualTo("(absent)");
    }

    @Test
    void capability_subFieldDrifted_qualityHint() {
        var driftedCap = AgentCapability.builder()
                .name("code-review").qualityHint(0.50).latencyHintP50Ms(2000L)
                .costHint("medium").inputTypes(List.of("text/plain"))
                .outputTypes(List.of("text/markdown")).tags(List.of("review"))
                .epistemicDomains(Map.of("java", 0.95)).excludedDomains(Set.of("cobol"))
                .build();
        var actual = withField(b -> b.capabilities(List.of(driftedCap)));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactly("capabilities[code-review].qualityHint");
        assertThat(result.drifts().get(0).desiredValue()).isEqualTo("0.85");
        assertThat(result.drifts().get(0).actualValue()).isEqualTo("0.5");
    }

    @Test
    void capability_epistemicDomainsDrifted() {
        var driftedCap = AgentCapability.builder()
                .name("code-review").qualityHint(0.85).latencyHintP50Ms(2000L)
                .costHint("medium").inputTypes(List.of("text/plain"))
                .outputTypes(List.of("text/markdown")).tags(List.of("review"))
                .epistemicDomains(Map.of("java", 0.90)).excludedDomains(Set.of("cobol"))
                .build();
        var actual = withField(b -> b.capabilities(List.of(driftedCap)));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactly("capabilities[code-review].epistemicDomains");
    }

    @Test
    void capability_excludedDomainsDrifted() {
        var driftedCap = AgentCapability.builder()
                .name("code-review").qualityHint(0.85).latencyHintP50Ms(2000L)
                .costHint("medium").inputTypes(List.of("text/plain"))
                .outputTypes(List.of("text/markdown")).tags(List.of("review"))
                .epistemicDomains(Map.of("java", 0.95)).excludedDomains(Set.of("fortran"))
                .build();
        var actual = withField(b -> b.capabilities(List.of(driftedCap)));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactly("capabilities[code-review].excludedDomains");
    }

    @Test
    void capability_orderIndependent() {
        var capA = AgentCapability.builder().name("alpha").build();
        var capB = AgentCapability.builder().name("beta").build();
        var desired = withField(b -> b.capabilities(List.of(capA, capB)));
        var actual = withField(b -> b.capabilities(List.of(capB, capA)));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.matches()).isTrue();
    }

    // --- Multiple simultaneous drifts ---

    @Test
    void multipleDrifts_allReported() {
        var driftedDisp = AgentDisposition.builder()
                .socialOrient("independent").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .conflictMode("compromising").delegation(false)
                .build();
        var actual = withField(b -> b.name("Bob").slot("planner").disposition(driftedDisp));
        var result = AgentDescriptorComparator.compare(base(), actual);
        assertThat(result.drifts()).extracting("field")
                .containsExactlyInAnyOrder("name", "slot", "disposition.socialOrient");
    }

    // --- Field path format ---

    @Test
    void fieldPaths_followConvention() {
        var driftedDisp = AgentDisposition.builder()
                .socialOrient("independent").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous")
                .conflictMode("compromising").delegation(true)
                .build();
        var extra = AgentCapability.builder().name("planning").build();
        var driftedCap = AgentCapability.builder()
                .name("code-review").qualityHint(0.50).latencyHintP50Ms(2000L)
                .costHint("medium").inputTypes(List.of("text/plain"))
                .outputTypes(List.of("text/markdown")).tags(List.of("review"))
                .epistemicDomains(Map.of("java", 0.95)).excludedDomains(Set.of("cobol"))
                .build();
        var actual = withField(b -> b
                .name("Bob")
                .disposition(driftedDisp)
                .axisVocabularies(Map.of(DispositionAxis.AUTONOMY, "urn:new"))
                .capabilities(List.of(driftedCap, extra)));

        var result = AgentDescriptorComparator.compare(base(), actual);
        var fields = result.drifts().stream().map(AgentDescriptorComparator.FieldDrift::field).toList();

        assertThat(fields).contains(
                "name",
                "disposition.socialOrient",
                "disposition.delegation",
                "axisVocabularies[AUTONOMY]",
                "axisVocabularies[SOCIAL_ORIENTATION]",
                "capabilities[code-review].qualityHint",
                "capabilities[planning]");
    }

    @Test
    void capability_vocabulary_drift_detected() {
        var desiredCap = AgentCapability.builder()
                .name("review")
                .capabilityVocabulary("urn:vocab:a")
                .build();
        var actualCap = AgentCapability.builder()
                .name("review")
                .capabilityVocabulary("urn:vocab:b")
                .build();
        var desired = withField(b -> b.capabilities(List.of(desiredCap)));
        var actual = withField(b -> b.capabilities(List.of(actualCap)));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.matches()).isFalse();
        assertThat(result.drifts()).anyMatch(d ->
            d.field().contains("capabilityVocabulary"));
    }

    @Test
    void comparatorCoversAllGoalComponents() {
        int total    = AgentGoal.class.getRecordComponents().length;
        int matchKey = 1;
        assertThat(AgentDescriptorComparator.COMPARED_GOAL_FIELD_COUNT).isEqualTo(total - matchKey);
    }

    @Test
    void comparatorCoversAllConstraintComponents() {
        int total    = AgentConstraint.class.getRecordComponents().length;
        int matchKey = 1;
        assertThat(AgentDescriptorComparator.COMPARED_CONSTRAINT_FIELD_COUNT).isEqualTo(total - matchKey);
    }

    @Test
    void goal_added() {
        var desired = withField(b -> b.goals(List.of(
                new AgentGoal("find-diamond", "Find it", GoalPriority.PRIMARY, Visibility.PUBLIC))));
        var actual = base();
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("goals[find-diamond]")
                                                  && d.desiredValue().equals("(present)") && d.actualValue().equals("(absent)"));
    }

    @Test
    void goal_removed() {
        var desired = base();
        var actual = withField(b -> b.goals(List.of(
                new AgentGoal("find-diamond", "Find it", GoalPriority.PRIMARY, Visibility.PUBLIC))));
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("goals[find-diamond]")
                                                  && d.desiredValue().equals("(absent)") && d.actualValue().equals("(present)"));
    }

    @Test
    void goal_description_drifted() {
        var g1      = new AgentGoal("g", "Old", GoalPriority.PRIMARY, Visibility.PUBLIC);
        var g2      = new AgentGoal("g", "New", GoalPriority.PRIMARY, Visibility.PUBLIC);
        var desired = withField(b -> b.goals(List.of(g1)));
        var actual  = withField(b -> b.goals(List.of(g2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("goals[g].description"));
    }

    @Test
    void goal_priority_drifted() {
        var g1      = new AgentGoal("g", "d", GoalPriority.PRIMARY, Visibility.PUBLIC);
        var g2      = new AgentGoal("g", "d", GoalPriority.SECONDARY, Visibility.PUBLIC);
        var desired = withField(b -> b.goals(List.of(g1)));
        var actual  = withField(b -> b.goals(List.of(g2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("goals[g].priority"));
    }

    @Test
    void goal_visibility_drifted() {
        var g1      = new AgentGoal("g", "d", GoalPriority.PRIMARY, Visibility.PUBLIC);
        var g2      = new AgentGoal("g", "d", GoalPriority.PRIMARY, Visibility.PRIVATE);
        var desired = withField(b -> b.goals(List.of(g1)));
        var actual  = withField(b -> b.goals(List.of(g2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("goals[g].visibility"));
    }

    @Test
    void constraint_added() {
        var desired = withField(b -> b.constraints(List.of(
                new AgentConstraint("no-violence", "desc", Visibility.PUBLIC, ConstraintSeverity.HARD))));
        var actual = base();
        var result = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("constraints[no-violence]")
                                                  && d.desiredValue().equals("(present)"));
    }

    @Test
    void constraint_description_drifted() {
        var c1      = new AgentConstraint("c", "Old", Visibility.PUBLIC, ConstraintSeverity.HARD);
        var c2      = new AgentConstraint("c", "New", Visibility.PUBLIC, ConstraintSeverity.HARD);
        var desired = withField(b -> b.constraints(List.of(c1)));
        var actual  = withField(b -> b.constraints(List.of(c2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("constraints[c].description"));
    }

    @Test
    void constraint_visibility_drifted() {
        var c1      = new AgentConstraint("c", "d", Visibility.PUBLIC, ConstraintSeverity.HARD);
        var c2      = new AgentConstraint("c", "d", Visibility.PRIVATE, ConstraintSeverity.HARD);
        var desired = withField(b -> b.constraints(List.of(c1)));
        var actual  = withField(b -> b.constraints(List.of(c2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("constraints[c].visibility"));
    }

    @Test
    void constraint_severity_drifted() {
        var c1      = new AgentConstraint("c", "d", Visibility.PUBLIC, ConstraintSeverity.HARD);
        var c2      = new AgentConstraint("c", "d", Visibility.PUBLIC, ConstraintSeverity.SOFT);
        var desired = withField(b -> b.constraints(List.of(c1)));
        var actual  = withField(b -> b.constraints(List.of(c2)));
        var result  = AgentDescriptorComparator.compare(desired, actual);
        assertThat(result.drifts()).anyMatch(d -> d.field().equals("constraints[c].severity"));
    }

}
