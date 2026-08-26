package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentSelectionTest {

    @Test
    void selectedCarriesAllFields() {
        var descriptor = AgentDescriptor.builder()
            .agentId("agent-1").name("Agent One").slot("reviewer").tenancyId("t1").build();
        var resolved = new ResolvedCapability(
            AgentCapability.builder().name("code-review").build(),
            new MatchDegree.Exact());
        var sel = new AgentSelection.Selected(descriptor, resolved, 0.85, "highest trust");

        assertEquals("agent-1", sel.agent().agentId());
        assertEquals(0.85, sel.trustScore());
        assertNotNull(sel.resolvedCapability());
        assertEquals("highest trust", sel.reason());
    }

    @Test
    void selectedAllowsNullResolvedCapability() {
        var descriptor = AgentDescriptor.builder()
            .agentId("agent-1").name("Agent One").slot("reviewer").tenancyId("t1").build();
        var sel = new AgentSelection.Selected(descriptor, null, 0.5, "slot query");
        assertNull(sel.resolvedCapability());
    }

    @Test
    void noneQualifiedCarriesReason() {
        var nq = new AgentSelection.NoneQualified("all unhealthy");
        assertEquals("all unhealthy", nq.reason());
    }

    @Test
    void escalatedCarriesKindAndReason() {
        var esc = new AgentSelection.Escalated("code-review",
            EscalationKind.BORDERLINE_STALEMATE, "all candidates borderline");
        assertEquals("code-review", esc.capabilityName());
        assertEquals(EscalationKind.BORDERLINE_STALEMATE, esc.kind());
    }

    @Test
    void patternMatchingExhaustive() {
        AgentSelection selection = new AgentSelection.NoneQualified("test");
        String result = switch (selection) {
            case AgentSelection.Selected s -> "selected: " + s.agent().agentId();
            case AgentSelection.NoneQualified nq -> "none: " + nq.reason();
            case AgentSelection.Escalated e -> "escalated: " + e.kind();
        };
        assertEquals("none: test", result);
    }
}
