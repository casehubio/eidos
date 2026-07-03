package io.casehub.eidos.runtime.registry;

import io.casehub.eidos.api.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class JpaAgentRegistryTest {

    @Inject
    AgentRegistry registry;

    @Inject
    VocabularyRegistry vocabularyRegistry;

    @BeforeEach
    void registerTestVocabulary() {
        // Register the test capability vocabulary before each test
        vocabularyRegistry.register(TestCapabilityVocab.class);
    }

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId,
                                      String... capabilityNames) {
        var caps = Arrays.stream(capabilityNames)
            .map(n -> AgentCapability.builder().name(n).qualityHint(0.9)
                .epistemicDomains(Map.of()).build())
            .toList();
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent " + agentId)
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot(slot)
            .capabilities(caps)
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId(tenancyId)
            .build();
    }

    @Test
    @TestTransaction
    void register_and_find_by_id() {
        registry.register(descriptor("agent-1", "reviewer", "default", "code-review"));

        var found = registry.findById("agent-1", "default");

        assertThat(found).isPresent();
        assertThat(found.get().agentId()).isEqualTo("agent-1");
        assertThat(found.get().slot()).isEqualTo("reviewer");
        assertThat(found.get().tenancyId()).isEqualTo("default");
        assertThat(found.get().capabilities()).hasSize(1);
        assertThat(found.get().capabilities().get(0).name()).isEqualTo("code-review");
    }

    @Test
    @TestTransaction
    void find_by_slot_returns_matching_agents_only() {
        registry.register(descriptor("agent-2a", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-2b", "planner", "default", "planning"));

        var reviewers = registry.find(AgentQuery.bySlot("reviewer", "default"));

        assertThat(reviewers).hasSize(1);
        assertThat(reviewers).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-2a");
        assertThat(reviewers).allSatisfy(m -> assertThat(m.resolvedCapability()).isNull());
    }

    @Test
    @TestTransaction
    void find_by_capability_returns_agents_with_that_capability() {
        registry.register(descriptor("agent-3a", "reviewer", "default", "code-review", "test-writing"));
        registry.register(descriptor("agent-3b", "executor", "default", "test-writing"));

        var codeReviewers = registry.find(AgentQuery.byCapability("code-review", "default"));

        assertThat(codeReviewers).hasSize(1);
        assertThat(codeReviewers).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-3a");
        assertThat(codeReviewers.getFirst().resolvedCapability()).isNotNull();
        assertThat(codeReviewers.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    @TestTransaction
    void find_by_slot_and_capability_applies_both_filters() {
        registry.register(descriptor("agent-4a", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-4b", "executor", "default", "code-review"));

        var result = registry.find(AgentQuery.bySlotAndCapability("reviewer", "code-review", "default"));

        assertThat(result).hasSize(1);
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-4a");
        assertThat(result.getFirst().resolvedCapability()).isNotNull();
        assertThat(result.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    @TestTransaction
    void register_upserts_existing_agent() {
        registry.register(descriptor("agent-5", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-5", "planner", "default", "planning"));

        var found = registry.findById("agent-5", "default");

        assertThat(found).isPresent();
        assertThat(found.get().slot()).isEqualTo("planner");
        assertThat(found.get().capabilities()).hasSize(1);
        assertThat(found.get().capabilities().get(0).name()).isEqualTo("planning");
    }

    @Test
    @TestTransaction
    void tenancy_isolation_excludes_other_tenant_agents() {
        registry.register(descriptor("agent-6", "reviewer", "tenant-a", "code-review"));

        var result = registry.find(AgentQuery.bySlot("reviewer", "tenant-b"));

        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    void find_all_returns_only_own_tenant() {
        registry.register(descriptor("agent-7a", "reviewer", "tenant-a", "code-review"));
        registry.register(descriptor("agent-7b", "planner", "tenant-b", "planning"));

        var tenantA = registry.find(AgentQuery.all("tenant-a"));

        assertThat(tenantA).hasSize(1);
        assertThat(tenantA).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-7a");
        assertThat(tenantA).allSatisfy(m -> assertThat(m.resolvedCapability()).isNull());
    }

    @Test
    @TestTransaction
    void find_by_id_returns_empty_for_missing_agent() {
        assertThat(registry.findById("nonexistent", "default")).isEmpty();
    }

    @Test
    @TestTransaction
    void findById_with_null_agentId_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> registry.findById(null, "default"))
            .withMessageContaining("agentId");
    }

    @Test
    @TestTransaction
    void findById_with_null_tenancyId_throws() {
        assertThatThrownBy(() -> registry.findById("nonexistent", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @TestTransaction
    void axis_vocabularies_round_trips_through_jpa() {
        var d = AgentDescriptor.builder()
            .agentId("agent-axis")
            .name("Axis Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slotVocabulary("urn:casehub:vocab:belbin")
            .dispositionVocabulary("urn:casehub:vocab:disc")
            .axisVocabularies(Map.of(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann"))
            .slot("co-ordinator")
            .capabilities(List.of())
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .conflictMode("collaborating")
                .delegation(true)
                .build())
            .tenancyId("default")
            .build();
        registry.register(d);
        var found = registry.findById("agent-axis", "default").orElseThrow();
        assertThat(found.axisVocabularies())
            .containsEntry(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann");
        assertThat(found.vocabUriForAxis(DispositionAxis.CONFLICT_MODE))
            .contains("urn:casehub:vocab:thomas-kilmann");
        assertThat(found.vocabUriForAxis(DispositionAxis.SOCIAL_ORIENTATION))
            .contains("urn:casehub:vocab:disc");
    }

    @Test
    @TestTransaction
    void null_axis_vocabularies_round_trips_as_null() {
        registry.register(descriptor("agent-noaxis", "reviewer", "default"));
        var found = registry.findById("agent-noaxis", "default").orElseThrow();
        assertThat(found.axisVocabularies()).isNull();
    }

    @Test
    @TestTransaction
    void domain_filter_excludes_agents_with_excluded_domain() {
        var caps = List.of(AgentCapability.builder()
            .name("code-review").qualityHint(0.9)
            .excludedDomains(Set.of("rust", "c++"))
            .build());
        registry.register(AgentDescriptor.builder()
            .agentId("agent-excl").name("Excluded").slot("reviewer")
            .capabilities(caps).tenancyId("default").build());
        registry.register(descriptor("agent-incl", "reviewer", "default", "code-review"));

        var result = registry.find(AgentQuery.byCapabilityAndDomain("code-review", "rust", "default"));

        assertThat(result).hasSize(1);
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-incl");
        assertThat(result.getFirst().resolvedCapability()).isNotNull();
        assertThat(result.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    @TestTransaction
    void domain_filter_includes_agents_without_exclusions() {
        registry.register(descriptor("agent-open", "reviewer", "default", "code-review"));

        var result = registry.find(AgentQuery.byCapabilityAndDomain("code-review", "java", "default"));

        assertThat(result).hasSize(1);
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .containsExactly("agent-open");
        assertThat(result.getFirst().resolvedCapability()).isNotNull();
        assertThat(result.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    @TestTransaction
    void excluded_domains_round_trips_through_jpa() {
        var caps = List.of(AgentCapability.builder()
            .name("code-review").qualityHint(0.9)
            .excludedDomains(Set.of("rust", "go"))
            .build());
        registry.register(AgentDescriptor.builder()
            .agentId("agent-rt").name("RT").slot("reviewer")
            .capabilities(caps).tenancyId("default").build());

        var found = registry.findById("agent-rt", "default").orElseThrow();
        assertThat(found.capabilities().get(0).excludedDomains())
            .containsExactlyInAnyOrder("rust", "go");
    }

    // --- Subsumption tests ---

    @Test
    @TestTransaction
    void find_by_capability_matches_via_subsumption() {
        // Register an agent with a general "review" capability grounded in TestCapabilityVocab
        var generalCap = AgentCapability.builder()
            .name("review")
            .capabilityVocabulary("urn:test:capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("agent-sub-1")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(generalCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        registry.register(desc);

        // Query for "security-review" — which is a specialization of "review"
        var result = registry.find(AgentQuery.byCapability("security-review", "default"));

        // The agent should be found via subsumption
        assertThat(result).extracting(m -> m.descriptor().agentId()).containsExactly("agent-sub-1");
        assertThat(result.getFirst().resolvedCapability()).isNotNull();
        assertThat(result.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Plugin.class);
    }

    @Test
    @TestTransaction
    void find_ungrounded_capability_uses_exact_match_only() {
        // Register an agent with capability "code-review" (no vocabulary)
        registry.register(descriptor("agent-exact-1", "reviewer", "default", "code-review"));

        // Query for "security-code-review"
        var result = registry.find(AgentQuery.byCapability("security-code-review", "default"));

        // Should NOT be found (no subsumption without vocabulary grounding)
        assertThat(result).extracting(m -> m.descriptor().agentId()).doesNotContain("agent-exact-1");
    }

    @Test
    @TestTransaction
    void register_rejects_unknown_capability_vocabulary() {
        var cap = AgentCapability.builder()
            .name("review")
            .capabilityVocabulary("urn:nonexistent:vocab")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("agent-invalid-vocab")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(cap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();

        assertThatThrownBy(() -> registry.register(desc))
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("capabilityVocabulary")
            .hasMessageContaining("urn:nonexistent:vocab");
    }

    @Test
    @TestTransaction
    void register_rejects_unknown_term_in_known_vocabulary() {
        var cap = AgentCapability.builder()
            .name("nonexistent-capability")
            .capabilityVocabulary("urn:test:capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("agent-invalid-term")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(cap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();

        assertThatThrownBy(() -> registry.register(desc))
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("capability.name")
            .hasMessageContaining("nonexistent-capability");
    }

    // --- Cross-vocabulary JPA registry tests ---

    /**
     * Foundation vocabulary for cross-vocabulary tests.
     * Simulates casehub-eidos-vocab/CasehubCapabilityTerm without adding a dependency.
     */
    @VocabularyMetadata(uri = "urn:test:foundation-cap", name = "Foundation Capability Vocabulary")
    public enum FoundationCapabilityVocab implements VocabularyTerm {
        DOCUMENTATION("documentation", "Documentation");

        private final String value;
        private final String label;

        FoundationCapabilityVocab(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override public String value() { return value; }
        @Override public String label() { return label; }
    }

    /**
     * App-tier vocabulary that specializes FoundationCapabilityVocab.DOCUMENTATION.
     * Tests cross-vocabulary subsumption in JPA query path.
     */
    @VocabularyMetadata(uri = "urn:test:app-clinical-cap", name = "Clinical Capability Vocabulary")
    public enum ClinicalCapabilityVocab implements VocabularyTerm {
        CLINICAL_DOCUMENTATION_REVIEW("clinical-documentation-review", "Clinical Documentation Review");

        private final String value;
        private final String label;

        ClinicalCapabilityVocab(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override public String value() { return value; }
        @Override public String label() { return label; }

        @Override public List<VocabularyTerm> specializes() {
            return List.of(FoundationCapabilityVocab.DOCUMENTATION);
        }
    }

    @Test
    @TestTransaction
    void cross_vocabulary_query_foundation_term_matches_app_tier_specialization_via_plugin() {
        // Register both foundation and cross-vocabulary vocab
        vocabularyRegistry.register(FoundationCapabilityVocab.class);
        vocabularyRegistry.register(ClinicalCapabilityVocab.class);

        // Register agent with app-tier clinical-documentation-review capability
        var clinicalCap = AgentCapability.builder()
            .name("clinical-documentation-review")
            .capabilityVocabulary("urn:test:app-clinical-cap")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var clinicalAgent = AgentDescriptor.builder()
            .agentId("agent-clinical")
            .name("Clinical Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("clinical-reviewer")
            .capabilities(List.of(clinicalCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        registry.register(clinicalAgent);

        // Query for foundation "documentation" term
        var result = registry.find(AgentQuery.byCapability("documentation", "default"));

        // JPA query path should match via Specialization (agent capability specializes query term)
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("agent-clinical");
        var match = result.stream()
            .filter(m -> m.descriptor().agentId().equals("agent-clinical"))
            .findFirst().orElseThrow();
        assertThat(match.resolvedCapability()).isNotNull();
        assertThat(match.resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Specialization.class);
    }

    @Test
    @TestTransaction
    void cross_vocabulary_query_app_tier_term_matches_foundation_via_specialization() {
        // Register both foundation and cross-vocabulary vocab
        vocabularyRegistry.register(FoundationCapabilityVocab.class);
        vocabularyRegistry.register(ClinicalCapabilityVocab.class);

        // Register agent with foundation documentation capability
        var foundationCap = AgentCapability.builder()
            .name("documentation")
            .capabilityVocabulary("urn:test:foundation-cap")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var foundationAgent = AgentDescriptor.builder()
            .agentId("agent-foundation-doc")
            .name("Foundation Documentation Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("general-reviewer")
            .capabilities(List.of(foundationCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        registry.register(foundationAgent);

        // Query for app-tier clinical-documentation-review
        var result = registry.find(AgentQuery.byCapability("clinical-documentation-review", "default"));

        // JPA query path should match via Plugin (agent capability is parent of query term)
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("agent-foundation-doc");
        var match = result.stream()
            .filter(m -> m.descriptor().agentId().equals("agent-foundation-doc"))
            .findFirst().orElseThrow();
        assertThat(match.resolvedCapability()).isNotNull();
        assertThat(match.resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Plugin.class);
    }

    @Test
    @TestTransaction
    void cross_vocabulary_jpa_matches_same_agents_as_in_memory() {
        // Register both foundation and cross-vocabulary vocab
        vocabularyRegistry.register(FoundationCapabilityVocab.class);
        vocabularyRegistry.register(ClinicalCapabilityVocab.class);

        // Register both foundation and app-tier agents
        var foundationCap = AgentCapability.builder()
            .name("documentation")
            .capabilityVocabulary("urn:test:foundation-cap")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        registry.register(AgentDescriptor.builder()
            .agentId("agent-foundation-consistency")
            .name("Foundation Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(foundationCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build());

        var appCap = AgentCapability.builder()
            .name("clinical-documentation-review")
            .capabilityVocabulary("urn:test:app-clinical-cap")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        registry.register(AgentDescriptor.builder()
            .agentId("agent-app-consistency")
            .name("App Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(appCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build());

        // Query for foundation term — should match both
        var foundationQuery = registry.find(AgentQuery.byCapability("documentation", "default"));
        assertThat(foundationQuery).extracting(m -> m.descriptor().agentId())
            .containsExactlyInAnyOrder("agent-foundation-consistency", "agent-app-consistency");
        assertThat(foundationQuery).allSatisfy(m -> {
            assertThat(m.resolvedCapability()).isNotNull();
            assertThat(m.resolvedCapability().degree()).satisfiesAnyOf(
                degree -> assertThat(degree).isInstanceOf(MatchDegree.Exact.class),
                degree -> assertThat(degree).isInstanceOf(MatchDegree.Specialization.class)
            );
        });

        // Query for app-tier term — should match both
        var appQuery = registry.find(AgentQuery.byCapability("clinical-documentation-review", "default"));
        assertThat(appQuery).extracting(m -> m.descriptor().agentId())
            .containsExactlyInAnyOrder("agent-foundation-consistency", "agent-app-consistency");
        assertThat(appQuery).allSatisfy(m -> {
            assertThat(m.resolvedCapability()).isNotNull();
            assertThat(m.resolvedCapability().degree()).satisfiesAnyOf(
                degree -> assertThat(degree).isInstanceOf(MatchDegree.Exact.class),
                degree -> assertThat(degree).isInstanceOf(MatchDegree.Plugin.class)
            );
        });
    }

}
