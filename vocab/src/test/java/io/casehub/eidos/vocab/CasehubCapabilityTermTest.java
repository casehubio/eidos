package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CasehubCapabilityTermTest {

    @Test
    void uri_constant_is_defined() {
        assertThat(CasehubCapabilityTerm.URI).isEqualTo("urn:casehub:vocab:capability");
    }

    @Test
    void sast_review_specializes_security_code_review_and_static_analysis() {
        assertThat(CasehubCapabilityTerm.SAST_REVIEW.specializes())
            .containsExactlyInAnyOrder(
                CasehubCapabilityTerm.SECURITY_CODE_REVIEW,
                CasehubCapabilityTerm.STATIC_ANALYSIS);
    }

    @Test
    void security_code_review_specializes_code_review() {
        assertThat(CasehubCapabilityTerm.SECURITY_CODE_REVIEW.specializes())
            .containsExactly(CasehubCapabilityTerm.CODE_REVIEW);
    }

    @Test
    void performance_code_review_specializes_code_review() {
        assertThat(CasehubCapabilityTerm.PERFORMANCE_CODE_REVIEW.specializes())
            .containsExactly(CasehubCapabilityTerm.CODE_REVIEW);
    }

    @Test
    void static_analysis_specializes_analysis() {
        assertThat(CasehubCapabilityTerm.STATIC_ANALYSIS.specializes())
            .containsExactly(CasehubCapabilityTerm.ANALYSIS);
    }

    @Test
    void root_terms_have_no_specializations() {
        assertThat(CasehubCapabilityTerm.CODE_REVIEW.specializes()).isEmpty();
        assertThat(CasehubCapabilityTerm.ANALYSIS.specializes()).isEmpty();
        assertThat(CasehubCapabilityTerm.TESTING.specializes()).isEmpty();
        assertThat(CasehubCapabilityTerm.DOCUMENTATION.specializes()).isEmpty();
    }

    @Test
    void all_terms_present() {
        var values = new String[] {
            CasehubCapabilityTerm.CODE_REVIEW.value(),
            CasehubCapabilityTerm.SECURITY_CODE_REVIEW.value(),
            CasehubCapabilityTerm.PERFORMANCE_CODE_REVIEW.value(),
            CasehubCapabilityTerm.SAST_REVIEW.value(),
            CasehubCapabilityTerm.ANALYSIS.value(),
            CasehubCapabilityTerm.STATIC_ANALYSIS.value(),
            CasehubCapabilityTerm.TESTING.value(),
            CasehubCapabilityTerm.DOCUMENTATION.value()
        };
        assertThat(values).contains("code-review", "security-code-review", "performance-code-review",
                                     "sast-review", "analysis", "static-analysis", "testing", "documentation");
    }

    @Test
    void sast_review_has_aliases() {
        assertThat(CasehubCapabilityTerm.SAST_REVIEW.aliases())
            .contains("static-security-review", "static-app-security-test");
    }

    @Test
    void static_analysis_has_alias() {
        assertThat(CasehubCapabilityTerm.STATIC_ANALYSIS.aliases())
            .contains("static-code-analysis");
    }
}
