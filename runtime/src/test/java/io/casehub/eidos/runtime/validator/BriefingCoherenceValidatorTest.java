package io.casehub.eidos.runtime.validator;

import io.casehub.eidos.api.*;
import io.casehub.eidos.vocab.*;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BriefingCoherenceValidatorTest {

    private BriefingCoherenceValidator validator;

    @BeforeEach
    void setUp() {
        var vocabRegistry = new CdiVocabularyRegistry();
        vocabRegistry.register(JungianFunctionTerm.class);
        vocabRegistry.register(ConscientiousnessTerm.class);
        vocabRegistry.register(ThomasKilmannTerm.class);
        validator = new BriefingCoherenceValidator(vocabRegistry);
    }

    @Test
    void noBriefing_returnsAligned() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .build();
        assertEquals(CoherenceReport.ALIGNED, validator.validateStructural(descriptor));
    }

    @Test
    void noDisposition_returnsAligned() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .briefing("A bold and daring agent")
            .build();
        assertEquals(CoherenceReport.ALIGNED, validator.validateStructural(descriptor));
    }

    @Test
    void briefingContradicts_riskAppetite_reportsTension() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .briefing("A bold risk-taker who charges ahead")
            .dispositionVocabulary(ConscientiousnessTerm.URI)
            .disposition(AgentDisposition.builder()
                .riskAppetite("conservative")
                .build())
            .build();
        var report = validator.validateStructural(descriptor);
        assertEquals(CoherenceLevel.TENSION, report.overall());
        assertFalse(report.violations().isEmpty());
        assertTrue(report.violations().stream()
            .anyMatch(v -> v.impliedValue().equals("bold")));
    }

    @Test
    void briefingAligned_returnsAligned() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .briefing("A careful and thorough approach to problems")
            .dispositionVocabulary(ConscientiousnessTerm.URI)
            .disposition(AgentDisposition.builder()
                .riskAppetite("conservative")
                .build())
            .build();
        var report = validator.validateStructural(descriptor);
        assertEquals(CoherenceLevel.ALIGNED, report.overall());
    }

    @Test
    void orientationContradiction_introvertedDominant_extravertedBriefing() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .briefing("An outgoing communicator who thinks out loud")
            .dispositionVocabulary(JungianFunctionTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(
                    new DispositionValue("ti", 0.45),
                    new DispositionValue("ne", 0.20))
                .build())
            .build();
        var report = validator.validateStructural(descriptor);
        assertEquals(CoherenceLevel.TENSION, report.overall());
        assertTrue(report.violations().stream()
            .anyMatch(v -> v.description().contains("orientation")));
    }

    @Test
    void orientationAligned_introvertedDominant_introvertedBriefing() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .briefing("A reserved analyst who works alone")
            .dispositionVocabulary(JungianFunctionTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(
                    new DispositionValue("ti", 0.45),
                    new DispositionValue("ne", 0.20))
                .build())
            .build();
        var report = validator.validateStructural(descriptor);
        assertTrue(report.violations().stream()
            .noneMatch(v -> v.description().contains("orientation")));
    }

    @Test
    void validate_delegatesToStructural() {
        var descriptor = AgentDescriptor.builder()
            .agentId("a1").name("Agent").tenancyId("t1").slot("tester")
            .build();
        assertEquals(validator.validateStructural(descriptor), validator.validate(descriptor));
    }
}
