package io.casehub.eidos.runtime.resolver;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.platform.agent.ResolvedAgent;
import io.casehub.platform.api.identity.CurrentPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EidosAgentResolverTest {

    private AgentRegistry registry;
    private SystemPromptRenderer renderer;
    private CurrentPrincipal principal;
    private EidosAgentResolver resolver;

    @BeforeEach
    void setUp() {
        registry = mock(AgentRegistry.class);
        renderer = mock(SystemPromptRenderer.class);
        principal = stubPrincipal("tenant-1");
        resolver = new EidosAgentResolver(registry, renderer, principal);
    }

    @Test
    void resolveReturnsRenderedPrompt() {
        var descriptor = minimalDescriptor("agent-1", "analyst");
        when(registry.findById("agent-1", "tenant-1")).thenReturn(Optional.of(descriptor));
        when(renderer.render(eq(descriptor), any()))
                .thenReturn(new RenderedPrompt("You are an analyst.", RenderFormat.MARKDOWN, "h1", "h2", false));

        var result = resolver.resolve("agent-1");

        assertThat(result).isPresent();
        var resolved = result.get();
        assertThat(resolved.agentId()).isEqualTo("agent-1");
        assertThat(resolved.systemPrompt()).isEqualTo("You are an analyst.");
    }

    @Test
    void resolvePopulatesMetadata() {
        var descriptor = AgentDescriptor.builder()
                .agentId("agent-1").name("Analyst").slot("reviewer").tenancyId("tenant-1")
                .domainVocabulary("urn:casehub:domain:legal")
                .archetype("Sage")
                .build();
        when(registry.findById("agent-1", "tenant-1")).thenReturn(Optional.of(descriptor));
        when(renderer.render(eq(descriptor), any()))
                .thenReturn(new RenderedPrompt("prompt", RenderFormat.MARKDOWN, "h1", "h2", false));

        var resolved = resolver.resolve("agent-1").orElseThrow();

        assertThat(resolved.metadata())
                .containsEntry("slot", "reviewer")
                .containsEntry("domainVocabulary", "urn:casehub:domain:legal")
                .containsEntry("archetype", "Sage");
    }

    @Test
    void resolveExtractsModelRefFromFirstCapability() {
        var descriptor = AgentDescriptor.builder()
                .agentId("agent-1").name("Coder").slot("dev").tenancyId("tenant-1")
                .capabilities(List.of(
                        AgentCapability.builder().name("code-review").modelRef("claude-opus-4-6").build(),
                        AgentCapability.builder().name("summarize").modelRef("claude-haiku-3-5").build()))
                .build();
        when(registry.findById("agent-1", "tenant-1")).thenReturn(Optional.of(descriptor));
        when(renderer.render(eq(descriptor), any()))
                .thenReturn(new RenderedPrompt("prompt", RenderFormat.MARKDOWN, "h1", "h2", false));

        var resolved = resolver.resolve("agent-1").orElseThrow();
        assertThat(resolved.model()).isEqualTo("claude-opus-4-6");
    }

    @Test
    void resolveReturnsNullModelWhenNoModelRef() {
        var descriptor = minimalDescriptor("agent-1", "worker");
        when(registry.findById("agent-1", "tenant-1")).thenReturn(Optional.of(descriptor));
        when(renderer.render(eq(descriptor), any()))
                .thenReturn(new RenderedPrompt("prompt", RenderFormat.MARKDOWN, "h1", "h2", false));

        var resolved = resolver.resolve("agent-1").orElseThrow();
        assertThat(resolved.model()).isNull();
    }

    @Test
    void resolveReturnsEmptyForUnknownAgent() {
        when(registry.findById("ghost", "tenant-1")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("ghost")).isEmpty();
        verifyNoInteractions(renderer);
    }

    @Test
    void resolveUsesMarkdownFormat() {
        var descriptor = minimalDescriptor("agent-1", "worker");
        when(registry.findById("agent-1", "tenant-1")).thenReturn(Optional.of(descriptor));
        when(renderer.render(eq(descriptor), any()))
                .thenReturn(new RenderedPrompt("prompt", RenderFormat.MARKDOWN, "h1", "h2", false));

        resolver.resolve("agent-1");

        var contextCaptor = org.mockito.ArgumentCaptor.forClass(AgentPromptContext.class);
        verify(renderer).render(eq(descriptor), contextCaptor.capture());
        assertThat(contextCaptor.getValue().format()).isEqualTo(RenderFormat.MARKDOWN);
    }

    @Test
    void discoverReturnsAllAgentsForTenant() {
        var d1 = minimalDescriptor("agent-1", "analyst");
        var d2 = AgentDescriptor.builder()
                .agentId("agent-2").name("Reviewer").slot("reviewer").tenancyId("tenant-1")
                .briefing("Reviews code quality")
                .build();
        when(registry.find(AgentQuery.all("tenant-1")))
                .thenReturn(List.of(new AgentMatch(d1, null), new AgentMatch(d2, null)));

        var result = resolver.discover();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).agentId()).isEqualTo("agent-1");
        assertThat(result.get(0).displayName()).isEqualTo("Agent 1");
        assertThat(result.get(1).agentId()).isEqualTo("agent-2");
        assertThat(result.get(1).displayName()).isEqualTo("Reviewer");
        assertThat(result.get(1).description()).isEqualTo("Reviews code quality");
    }

    @Test
    void discoverPropagatesSlotInMetadata() {
        var descriptor = AgentDescriptor.builder()
                .agentId("agent-1").name("Analyst").slot("analyst").tenancyId("tenant-1")
                .domainVocabulary("urn:casehub:domain:legal")
                .build();
        when(registry.find(AgentQuery.all("tenant-1")))
                .thenReturn(List.of(new AgentMatch(descriptor, null)));

        var result = resolver.discover();

        assertThat(result.getFirst().metadata())
                .containsEntry("slot", "analyst")
                .containsEntry("domainVocabulary", "urn:casehub:domain:legal");
    }

    @Test
    void discoverReturnsEmptyListWhenNoAgents() {
        when(registry.find(AgentQuery.all("tenant-1"))).thenReturn(List.of());
        assertThat(resolver.discover()).isEmpty();
    }

    private static AgentDescriptor minimalDescriptor(String agentId, String slot) {
        var raw = agentId.replace("-", " ");
        var name = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        return AgentDescriptor.builder()
                .agentId(agentId).name(name)
                .slot(slot).tenancyId("tenant-1")
                .build();
    }

    private static CurrentPrincipal stubPrincipal(String tenancyId) {
        return new CurrentPrincipal() {
            @Override public String actorId() { return "test-user"; }
            @Override public Set<String> groups() { return Set.of(); }
            @Override public String tenancyId() { return tenancyId; }
            @Override public boolean isCrossTenantAdmin() { return false; }
        };
    }
}
