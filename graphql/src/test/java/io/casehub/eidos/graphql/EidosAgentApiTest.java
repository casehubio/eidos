package io.casehub.eidos.graphql;

import io.casehub.eidos.api.*;
import io.casehub.platform.api.identity.CurrentPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EidosAgentApiTest {

    @Mock AgentRegistry registry;
    @Mock CurrentPrincipal principal;

    EidosAgentApi api;

    static final String TENANT = "test-tenant";

    @BeforeEach
    void setUp() {
        when(principal.tenancyId()).thenReturn(TENANT);
        api = new EidosAgentApi(registry, principal);
    }

    @Test
    void findAgents_noParams_listsAll() {
        var descriptor = testDescriptor("agent-1");
        when(registry.find(any(AgentQuery.class)))
            .thenReturn(List.of(new AgentMatch(descriptor, null)));

        var result = api.findAgents(null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().agentId()).isEqualTo("agent-1");
    }

    @Test
    void findAgents_withSlot_passesQueryParam() {
        when(registry.find(any(AgentQuery.class)))
            .thenReturn(List.of());

        api.findAgents("planner", null, null, null, null);

        verify(registry).find(
            org.mockito.ArgumentMatchers.argThat(q ->
                "planner".equals(q.slot()) && TENANT.equals(q.tenancyId())));
    }

    @Test
    void findAgents_filtersPrivateGoals() {
        var descriptor = AgentDescriptor.builder()
            .agentId("agent-1").name("Agent One").slot("tester").tenancyId(TENANT)
            .goals(List.of(
                new AgentGoal("public-goal", "visible", GoalPriority.PRIMARY,
                    Visibility.PUBLIC, List.of(), null),
                new AgentGoal("private-goal", "hidden", GoalPriority.SECONDARY,
                    Visibility.PRIVATE, List.of(), null)))
            .constraints(List.of(
                new AgentConstraint("public-constraint", "visible",
                    Visibility.PUBLIC, ConstraintSeverity.HARD),
                new AgentConstraint("private-constraint", "hidden",
                    Visibility.PRIVATE, ConstraintSeverity.SOFT)))
            .build();
        when(registry.find(any(AgentQuery.class)))
            .thenReturn(List.of(new AgentMatch(descriptor, null)));

        var result = api.findAgents(null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().goals()).hasSize(1);
        assertThat(result.getFirst().goals().getFirst().name()).isEqualTo("public-goal");
        assertThat(result.getFirst().constraints()).hasSize(1);
        assertThat(result.getFirst().constraints().getFirst().name()).isEqualTo("public-constraint");
    }

    @Test
    void getAgent_found_returnsFiltered() {
        var descriptor = testDescriptor("agent-1");
        when(registry.findById("agent-1", TENANT))
            .thenReturn(Optional.of(descriptor));

        var result = api.getAgent("agent-1");

        assertThat(result).isNotNull();
        assertThat(result.agentId()).isEqualTo("agent-1");
    }

    @Test
    void getAgent_notFound_returnsNull() {
        when(registry.findById("unknown", TENANT))
            .thenReturn(Optional.empty());

        var result = api.getAgent("unknown");

        assertThat(result).isNull();
    }

    private AgentDescriptor testDescriptor(String id) {
        return AgentDescriptor.builder()
            .agentId(id).name("Test " + id).slot("tester").tenancyId(TENANT)
            .build();
    }
}
