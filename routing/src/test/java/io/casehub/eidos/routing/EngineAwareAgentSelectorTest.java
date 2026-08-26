package io.casehub.eidos.routing;

import io.casehub.api.spi.routing.AgentRoutingStrategy;
import io.casehub.api.spi.routing.EscalationReason;
import io.casehub.api.spi.routing.RoutingResult;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.ledger.api.spi.TrustScoreSource;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EngineAwareAgentSelectorTest {

    private CapabilityHealth healthMock;
    private AgentRoutingStrategy strategyMock;
    private Instance<AgentRoutingStrategy> strategyInstance;
    private TrustScoreSource trustSourceMock;
    private Instance<TrustScoreSource> trustSourceInstance;
    private EngineAwareAgentSelector selector;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        healthMock = mock(CapabilityHealth.class);
        strategyMock = mock(AgentRoutingStrategy.class);
        strategyInstance = mock(Instance.class);
        when(strategyInstance.get()).thenReturn(strategyMock);
        trustSourceMock = mock(TrustScoreSource.class);
        trustSourceInstance = mock(Instance.class);
        when(trustSourceInstance.isResolvable()).thenReturn(true);
        when(trustSourceInstance.get()).thenReturn(trustSourceMock);
        selector = new EngineAwareAgentSelector(healthMock, strategyInstance, trustSourceInstance);
    }

    @Test
    void emptyListReturnsNoneQualified() {
        var result = selector.select(List.of(), SelectionContext.of("t1", "code-review"));
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void selectedResultMapsCorrectly() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.assigned("agent-1", "trust qualified"));
        when(trustSourceMock.capabilityScore("agent-1", "code-review"))
            .thenReturn(OptionalDouble.of(0.9));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.Selected.class, result);
        var selected = (AgentSelection.Selected) result;
        assertEquals("agent-1", selected.agent().agentId());
        assertEquals(0.9, selected.trustScore());
        assertEquals("trust qualified", selected.reason());
    }

    @Test
    void unresolvableMapsToNoneQualified() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.unresolvable("no viable candidates"));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.NoneQualified.class, result);
        assertEquals("no viable candidates", ((AgentSelection.NoneQualified) result).reason());
    }

    @Test
    void escalatedMapsWithBorderlineKind() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.escalate("code-review",
                EscalationReason.BORDERLINE_STALEMATE, "all borderline"));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.Escalated.class, result);
        var esc = (AgentSelection.Escalated) result;
        assertEquals(EscalationKind.BORDERLINE_STALEMATE, esc.kind());
        assertEquals("code-review", esc.capabilityName());
    }

    @Test
    void escalatedMapsWithNoQualifiedKind() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.escalate("code-review",
                EscalationReason.NO_QUALIFIED_AGENT, "none qualified"));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.Escalated.class, result);
        assertEquals(EscalationKind.NO_QUALIFIED_AGENT,
            ((AgentSelection.Escalated) result).kind());
    }

    @Test
    void unavailableCandidatesFilteredBeforeDelegation() {
        var m1 = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        var m2 = matchWith("agent-2", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(argThat(d -> d != null && d.agentId().equals("agent-1")), any(), any()))
            .thenReturn(new CapabilityStatus.Unavailable("down"));
        when(healthMock.probe(argThat(d -> d != null && d.agentId().equals("agent-2")), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.assigned("agent-2", "only viable"));
        when(trustSourceMock.capabilityScore("agent-2", "code-review"))
            .thenReturn(OptionalDouble.of(0.7));

        var result = selector.select(List.of(m1, m2), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.Selected.class, result);
        verify(strategyMock).select(any(), argThat(list -> list.size() == 1));
    }

    @Test
    void allFilteredReturnsNoneQualified() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Unavailable("down"));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.NoneQualified.class, result);
        verifyNoInteractions(strategyMock);
    }

    @Test
    void healthMappingDegradedPassesThrough() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Degraded(DegradationReason.OVERLOADED, "slow"));
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.assigned("agent-1", "degraded but viable"));
        when(trustSourceMock.capabilityScore("agent-1", "code-review"))
            .thenReturn(OptionalDouble.of(0.6));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        assertInstanceOf(AgentSelection.Selected.class, result);
        verify(strategyMock).select(any(), argThat(list ->
            list.getFirst().health() == io.casehub.api.spi.routing.AgentHealth.DEGRADED));
    }

    @Test
    void trustScoreFallsBackToGlobal() {
        var match = matchWith("agent-1", "code-review", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(strategyMock.select(any(), anyList()))
            .thenReturn(RoutingResult.assigned("agent-1", "selected"));
        when(trustSourceMock.capabilityScore("agent-1", "code-review"))
            .thenReturn(OptionalDouble.empty());
        when(trustSourceMock.globalScore("agent-1"))
            .thenReturn(OptionalDouble.of(0.75));

        var result = selector.select(List.of(match), SelectionContext.of("t1", "code-review"));

        var selected = (AgentSelection.Selected) result;
        assertEquals(0.75, selected.trustScore());
    }

    private AgentMatch matchWith(String agentId, String capName, MatchDegree degree) {
        var descriptor = AgentDescriptor.builder()
            .agentId(agentId).name(agentId).slot("worker").tenancyId("t1")
            .capabilities(List.of(AgentCapability.builder().name(capName).build()))
            .build();
        var resolved = new ResolvedCapability(
            AgentCapability.builder().name(capName).build(), degree);
        return new AgentMatch(descriptor, resolved);
    }
}
