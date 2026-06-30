package io.casehub.eidos.api;

import java.util.List;

@VocabularyMetadata(uri = "urn:test:capabilities", name = "Test Capability Vocabulary")
public enum TestCapabilityVocab implements VocabularyTerm {
    REVIEW("review", "Review"),
    CODE_REVIEW("code-review", "Code Review", REVIEW),
    SECURITY_REVIEW("security-review", "Security Review", CODE_REVIEW),
    DESIGN_REVIEW("design-review", "Design Review", REVIEW),
    TESTING("testing", "Testing"),
    UNIT_TESTING("unit-testing", "Unit Testing", TESTING),
    INTEGRATION_TESTING("integration-testing", "Integration Testing", TESTING);

    private final String value;
    private final String label;
    private final TestCapabilityVocab parent;

    TestCapabilityVocab(String value, String label) {
        this(value, label, null);
    }

    TestCapabilityVocab(String value, String label, TestCapabilityVocab parent) {
        this.value = value;
        this.label = label;
        this.parent = parent;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public List<VocabularyTerm> specializes() {
        return parent != null ? List.of(parent) : List.of();
    }
}
