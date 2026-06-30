package io.casehub.eidos.examples;

import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.vocab.CasehubCapabilityTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CapabilityVocabularyIntegrationTest {

    @Inject VocabularyRegistry registry;

    @Test
    void capability_vocabulary_is_registered() {
        assertThat(registry.isRegistered(CasehubCapabilityTerm.URI)).isTrue();
    }

    @Test
    void sast_review_specializes_security_code_review_and_static_analysis() {
        assertThat(CasehubCapabilityTerm.SAST_REVIEW.specializes())
            .containsExactlyInAnyOrder(
                CasehubCapabilityTerm.SECURITY_CODE_REVIEW,
                CasehubCapabilityTerm.STATIC_ANALYSIS);
    }

    @Test
    void code_review_subsumes_sast_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "code-review", "sast-review")).isTrue();
    }

    @Test
    void analysis_subsumes_sast_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "analysis", "sast-review")).isTrue();
    }

    @Test
    void sast_review_does_not_subsume_code_review() {
        assertThat(registry.subsumes(CasehubCapabilityTerm.URI,
            "sast-review", "code-review")).isFalse();
    }

    @Test
    void root_terms_have_no_ancestors() {
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "code-review")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "analysis")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "testing")).isEmpty();
        assertThat(registry.ancestors(CasehubCapabilityTerm.URI, "documentation")).isEmpty();
    }

    @Test
    void sast_review_has_four_ancestors() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "sast-review");
        // sast-review specializes both SECURITY_CODE_REVIEW and STATIC_ANALYSIS
        // Direct specializations: SECURITY_CODE_REVIEW, STATIC_ANALYSIS
        // Transitive specializations: CODE_REVIEW (from SECURITY_CODE_REVIEW), ANALYSIS (from STATIC_ANALYSIS)
        assertThat(ancestors).hasSize(4)
            .extracting(VocabularyTerm::value)
            .containsExactlyInAnyOrder("security-code-review", "static-analysis", "code-review", "analysis");
    }

    @Test
    void static_analysis_has_analysis_as_ancestor() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "static-analysis");
        assertThat(ancestors).hasSize(1)
            .extracting(VocabularyTerm::value)
            .contains("analysis");
    }

    @Test
    void security_code_review_has_code_review_as_ancestor() {
        var ancestors = registry.ancestors(CasehubCapabilityTerm.URI, "security-code-review");
        assertThat(ancestors).hasSize(1)
            .extracting(VocabularyTerm::value)
            .contains("code-review");
    }

    @Test
    void all_capability_terms_are_resolvable() {
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "security-code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "performance-code-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "sast-review")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "analysis")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-analysis")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "testing")).isPresent();
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "documentation")).isPresent();
    }

    @Test
    void capability_aliases_are_resolvable() {
        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-app-security-test"))
            .isPresent()
            .get()
            .extracting(VocabularyTerm::value)
            .isEqualTo("sast-review");

        assertThat(registry.resolve(CasehubCapabilityTerm.URI, "static-code-analysis"))
            .isPresent()
            .get()
            .extracting(VocabularyTerm::value)
            .isEqualTo("static-analysis");
    }
}
