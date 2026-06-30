package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class DefaultCapabilityHealthTest {

    @Inject
    CapabilityHealth health;

    @Inject
    VocabularyRegistry vocabRegistry;

    // Test vocabulary for capability hierarchy
    @VocabularyMetadata(uri = "urn:test:health-capabilities", name = "Test Capabilities", version = "1.0")
    enum TestCapabilityTerm implements VocabularyTerm {
        CODE_REVIEW("code-review", "Code Review"),
        SECURITY_CODE_REVIEW("security-code-review", "Security Code Review") {
            @Override public List<VocabularyTerm> specializes() { return List.of(CODE_REVIEW); }
        },
        SAST_REVIEW("sast-review", "SAST Review") {
            @Override public List<VocabularyTerm> specializes() { return List.of(SECURITY_CODE_REVIEW); }
        };

        final String value, label;
        TestCapabilityTerm(String v, String l) { value = v; label = l; }
        @Override public String value() { return value; }
        @Override public String label() { return label; }
    }

    private void ensureVocabRegistered() {
        if (!vocabRegistry.isRegistered("urn:test:health-capabilities")) {
            vocabRegistry.register(TestCapabilityTerm.class);
        }
    }

    static AgentDescriptor agent(String agentId, AgentCapability... capabilities) {
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(capabilities))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
    }

    static AgentCapability capability(String name, Map<String, Double> epistemicDomains) {
        return AgentCapability.builder().name(name).qualityHint(0.9)
            .epistemicDomains(epistemicDomains).build();
    }

    @Test
    void returns_ready_when_capability_declared_and_no_task_domain() {
        var descriptor = agent("a1", capability("code-review", Map.of()));
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_unavailable_when_capability_not_declared() {
        var descriptor = agent("a2", capability("code-review", Map.of()));
        var status = health.probe(descriptor, "test-writing", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
        assertThat(((CapabilityStatus.Unavailable) status).reason()).contains("test-writing");
    }

    @Test
    void returns_ready_when_epistemic_domain_above_threshold() {
        var descriptor = agent("a3", capability("code-review", Map.of("java", 0.95)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_epistemically_weak_when_domain_below_threshold() {
        var descriptor = agent("a4", capability("code-review", Map.of("rust", 0.2)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(CapabilityStatus.EpistemicallyWeak.class);
        var weak = (CapabilityStatus.EpistemicallyWeak) status;
        assertThat(weak.domain()).isEqualTo("rust");
        assertThat(weak.confidence()).isEqualTo(0.2);
    }

    @Test
    void returns_ready_when_task_domain_not_in_epistemic_map() {
        var descriptor = agent("a5", capability("code-review", Map.of("java", 0.95)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("python"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_ready_when_epistemic_domains_null() {
        var descriptor = agent("a6", capability("code-review", null));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("java"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_ready_when_confidence_exactly_at_threshold() {
        var descriptor = agent("a7", capability("code-review", Map.of("go", 0.3)));
        var status = health.probe(descriptor, "code-review", ProbeContext.of("go"));
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void returns_unavailable_for_agent_with_no_capabilities() {
        var descriptor = agent("a8");
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
    }

    @Test
    void probe_finds_capability_via_subsumption() {
        ensureVocabRegistered();
        // Agent declares "code-review" grounded in test vocab
        // Probe for "security-code-review" (child of code-review in the vocab)
        var codeReview = AgentCapability.builder()
            .name("code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.9)
            .build();
        var descriptor = agent("a9", codeReview);
        var status = health.probe(descriptor, "security-code-review", ProbeContext.of(null));
        // Should find code-review via subsumption and return Ready (not Unavailable)
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_prefers_exact_match_over_subsumption() {
        ensureVocabRegistered();
        // Agent declares both "code-review" and "security-code-review"
        var codeReview = AgentCapability.builder()
            .name("code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of("java", 0.8))
            .build();
        var securityReview = AgentCapability.builder()
            .name("security-code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.95)
            .epistemicDomains(Map.of("java", 0.95))
            .build();
        var descriptor = agent("a10", codeReview, securityReview);
        var status = health.probe(descriptor, "security-code-review", ProbeContext.of("java"));
        // Should use the exact match
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
        // We can't directly verify which capability was selected, but we can verify Ready is returned
    }

    @Test
    void probe_selects_closest_subsumption_match() {
        ensureVocabRegistered();
        // Agent declares "code-review" (depth 2) and "security-code-review" (depth 1)
        // Probe for "sast-review" (child of security-code-review)
        var codeReview = AgentCapability.builder()
            .name("code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of("java", 0.8))
            .build();
        var securityReview = AgentCapability.builder()
            .name("security-code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.95)
            .epistemicDomains(Map.of("java", 0.95))
            .build();
        var descriptor = agent("a11", codeReview, securityReview);
        var status = health.probe(descriptor, "sast-review", ProbeContext.of("java"));
        // Should match security-code-review (closer) and return Ready
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }

    @Test
    void probe_ungrounded_capability_uses_exact_only() {
        ensureVocabRegistered();
        // Agent declares "code-review" without vocabulary
        var codeReview = AgentCapability.builder()
            .name("code-review")
            .qualityHint(0.9)
            .build();
        var descriptor = agent("a12", codeReview);
        var status = health.probe(descriptor, "security-code-review", ProbeContext.of(null));
        // Should not match via subsumption, return Unavailable
        assertThat(status).isInstanceOf(CapabilityStatus.Unavailable.class);
    }

    @Test
    void probe_matches_via_specialization() {
        ensureVocabRegistered();
        // Agent declares "security-code-review" (specific capability)
        // Probe for "code-review" (general capability — parent in hierarchy)
        var securityReview = AgentCapability.builder()
            .name("security-code-review")
            .capabilityVocabulary("urn:test:health-capabilities")
            .qualityHint(0.95)
            .build();
        var descriptor = agent("a13", securityReview);
        var status = health.probe(descriptor, "code-review", ProbeContext.of(null));
        // Should match via Specialization (agent declares more specific capability than requested)
        assertThat(status).isInstanceOf(CapabilityStatus.Ready.class);
    }
}
