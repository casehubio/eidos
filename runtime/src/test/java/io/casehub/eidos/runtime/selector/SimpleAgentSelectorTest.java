package io.casehub.eidos.runtime.selector;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.ledger.api.spi.TrustScoreSource;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleAgentSelectorTest {

    private CapabilityHealth healthMock;
    private TrustScoreSource trustSourceMock;
    private Instance<TrustScoreSource> trustSourceInstance;
    private SimpleAgentSelector selector;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        healthMock = mock(CapabilityHealth.class);
        trustSourceMock = mock(TrustScoreSource.class);
        trustSourceInstance = mock(Instance.class);
        when(trustSourceInstance.isResolvable()).thenReturn(true);
        when(trustSourceInstance.get()).thenReturn(trustSourceMock);
        selector = new SimpleAgentSelector(healthMock, trustSourceInstance, 0.0, 0.5);
    }

    @Test
    void emptyListReturnsNoneQualified() {
        var result = selector.select(List.of(), SelectionContext.of("t1", "cap"));
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void allUnhealthyReturnsNoneQualified() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Unavailable("down"));
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void singleHealthyCandidateSelected() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.of(0.8));
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.Selected.class, result);
        var selected = (AgentSelection.Selected) result;
        assertEquals("agent-1", selected.agent().agentId());
        assertEquals(0.8, selected.trustScore());
    }

    @Test
    void highestTrustScoreWins() {
        var m1 = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        var m2 = matchWith("agent-2", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.of(0.6));
        when(trustSourceMock.capabilityScore("agent-2", "cap-1"))
            .thenReturn(OptionalDouble.of(0.9));
        var result = selector.select(List.of(m1, m2), SelectionContext.of("t1", "cap-1"));
        var selected = (AgentSelection.Selected) result;
        assertEquals("agent-2", selected.agent().agentId());
    }

    @Test
    void tieBreakByMatchDegree() {
        var m1 = matchWith("agent-1", "cap-1", new MatchDegree.Plugin(1));
        var m2 = matchWith("agent-2", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore(any(), eq("cap-1")))
            .thenReturn(OptionalDouble.of(0.8));
        var result = selector.select(List.of(m1, m2), SelectionContext.of("t1", "cap-1"));
        var selected = (AgentSelection.Selected) result;
        assertEquals("agent-2", selected.agent().agentId());
    }

    @Test
    void capabilityToGlobalFallback() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.empty());
        when(trustSourceMock.globalScore("agent-1"))
            .thenReturn(OptionalDouble.of(0.85));
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        var selected = (AgentSelection.Selected) result;
        assertEquals(0.85, selected.trustScore());
    }

    @Test
    void trueBootstrapUsesDefaultScore() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore(any(), any()))
            .thenReturn(OptionalDouble.empty());
        when(trustSourceMock.globalScore(any()))
            .thenReturn(OptionalDouble.empty());
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        var selected = (AgentSelection.Selected) result;
        assertEquals(0.5, selected.trustScore());
    }

    @SuppressWarnings("unchecked")
    @Test
    void noTrustSourceFallsBackToHealthOnlyMode() {
        var noTrustInstance = (Instance<TrustScoreSource>) mock(Instance.class);
        when(noTrustInstance.isResolvable()).thenReturn(false);
        var selectorNoTrust = new SimpleAgentSelector(healthMock, noTrustInstance, 0.0, 0.5);
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        var result = selectorNoTrust.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        var selected = (AgentSelection.Selected) result;
        assertEquals(0.0, selected.trustScore());
    }

    @Test
    void thresholdFiltersLowScoreCandidates() {
        var thresholdSelector = new SimpleAgentSelector(healthMock, trustSourceInstance, 0.7, 0.5);
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.of(0.3));
        var result = thresholdSelector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void neverReturnsEscalated() {
        var thresholdSelector = new SimpleAgentSelector(healthMock, trustSourceInstance, 0.99, 0.5);
        var m1 = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), any(), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore(any(), any()))
            .thenReturn(OptionalDouble.of(0.5));
        var result = thresholdSelector.select(List.of(m1), SelectionContext.of("t1", "cap-1"));
        assertFalse(result instanceof AgentSelection.Escalated);
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void degradedAgentKeptInPool() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Degraded(DegradationReason.OVERLOADED, "slow"));
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.of(0.7));
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.Selected.class, result);
    }

    @Test
    void overloadedAgentFilteredOut() {
        var match = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(any(), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Overloaded(0.95, 0.8));
        when(trustSourceMock.capabilityScore("agent-1", "cap-1"))
            .thenReturn(OptionalDouble.of(0.9));
        var result = selector.select(List.of(match), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.NoneQualified.class, result);
    }

    @Test
    void mixOfHealthyAndOverloadedSelectsHealthy() {
        var m1 = matchWith("agent-1", "cap-1", new MatchDegree.Exact());
        var m2 = matchWith("agent-2", "cap-1", new MatchDegree.Exact());
        when(healthMock.probe(eq(m1.descriptor()), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Overloaded(0.9, 0.8));
        when(healthMock.probe(eq(m2.descriptor()), eq("cap-1"), any()))
            .thenReturn(new CapabilityStatus.Ready());
        when(trustSourceMock.capabilityScore("agent-2", "cap-1"))
            .thenReturn(OptionalDouble.of(0.7));
        var result = selector.select(List.of(m1, m2), SelectionContext.of("t1", "cap-1"));
        assertInstanceOf(AgentSelection.Selected.class, result);
        assertEquals("agent-2", ((AgentSelection.Selected) result).agent().agentId());
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
