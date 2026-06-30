package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:capability",
                    name = "CaseHub Capability Taxonomy", version = "1.0",
                    description = "CaseHub's native capability vocabulary defining software engineering capabilities across code review, analysis, testing, and documentation domains. Establishes a foundational taxonomy for agent skill declaration.")
public enum CasehubCapabilityTerm implements VocabularyTerm {

    CODE_REVIEW("code-review", "Code Review",
        "Review and evaluate code for correctness, quality, and adherence to standards",
        List.of()),

    SECURITY_CODE_REVIEW("security-code-review", "Security Code Review",
        "Specialized code review focused on identifying security vulnerabilities, threats, and attack surfaces",
        List.of()) {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(CODE_REVIEW);
        }
    },

    PERFORMANCE_CODE_REVIEW("performance-code-review", "Performance Code Review",
        "Specialized code review focused on identifying performance bottlenecks and optimization opportunities",
        List.of()) {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(CODE_REVIEW);
        }
    },

    SAST_REVIEW("sast-review", "Static Application Security Testing Review",
        "Specialized review combining static analysis with security-focused assessment via SAST tooling",
        List.of("static-security-review", "static-app-security-test")) {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(SECURITY_CODE_REVIEW, STATIC_ANALYSIS);
        }
    },

    ANALYSIS("analysis", "Analysis",
        "Systematic examination and investigation of code structure, semantics, and characteristics",
        List.of()),

    STATIC_ANALYSIS("static-analysis", "Static Analysis",
        "Automated analysis of code without execution to detect patterns, defects, and quality issues",
        List.of("static-code-analysis")) {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(ANALYSIS);
        }
    },

    TESTING("testing", "Testing",
        "Verification and validation of software behavior through test design and execution",
        List.of()),

    DOCUMENTATION("documentation", "Documentation",
        "Creation and maintenance of written specifications, guides, and technical records",
        List.of());

    public static final String URI = "urn:casehub:vocab:capability";

    private final String value, label, description;
    private final List<String> aliases;

    CasehubCapabilityTerm(String value, String label, String description, List<String> aliases) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.aliases = aliases;
    }

    @Override public String value()         { return value; }
    @Override public String label()         { return label; }
    @Override public String description()   { return description; }
    @Override public List<String> aliases() { return aliases; }
}
