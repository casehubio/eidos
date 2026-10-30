package io.casehub.eidos.org.runtime.prompt;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.PromptTier;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.org.api.OrgRegistry;
import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgPromptContributorTest {

    @Test void noRegistryProducesNoBlocks() {
        @SuppressWarnings("unchecked")
        Instance<OrgRegistry> absent = mock(Instance.class);
        when(absent.isResolvable()).thenReturn(false);

        var contributor = new OrgPromptContributor(absent);
        var descriptor = AgentDescriptor.builder()
                .agentId("agent-1").name("Agent One").slot("worker").tenancyId("t1").build();
        var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        assertThat(contributor.contribute(descriptor, context)).isEmpty();
    }

    @Test void noMembershipProducesNoBlocks() {
        var registry = new InMemoryOrgRegistry();

        @SuppressWarnings("unchecked")
        Instance<OrgRegistry> present = mock(Instance.class);
        when(present.isResolvable()).thenReturn(true);
        when(present.get()).thenReturn(registry);

        var contributor = new OrgPromptContributor(present);
        var descriptor = AgentDescriptor.builder()
                .agentId("stranger").name("Stranger").slot("worker").tenancyId("acme").build();
        var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        assertThat(contributor.contribute(descriptor, context)).isEmpty();
    }

    @Test void rendersFullOrgContext() {
        var registry = new InMemoryOrgRegistry();

        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("department")
                .member("alice", "lead")
                .member("bob", "dev")
                .member("carol", "dev")
                .add()
            .supervises("alice", "bob").add()
            .supervises("alice", "carol").add()
            .escalatesTo("bob", "alice").add()
            .escalatesTo("alice", "cto").add()
            .build();
        org.registerAll(registry);

        @SuppressWarnings("unchecked")
        Instance<OrgRegistry> present = mock(Instance.class);
        when(present.isResolvable()).thenReturn(true);
        when(present.get()).thenReturn(registry);

        var contributor = new OrgPromptContributor(present);
        var descriptor = AgentDescriptor.builder()
                .agentId("bob").name("Bob").slot("dev").tenancyId("acme").build();
        var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        var blocks = contributor.contribute(descriptor, context);
        assertThat(blocks).hasSize(1);

        var block = blocks.getFirst();
        assertThat(block.tier()).isEqualTo(PromptTier.COGNITIVE);
        assertThat(block.tag()).isEqualTo("ORG_CONTEXT");

        var content = block.content();
        assertThat(content).contains("You are a member of \"Engineering\" (department).");
        assertThat(content).contains("Your supervisor is alice.");
        assertThat(content).contains("To escalate unresolved issues, contact: alice");
        assertThat(content).contains("Your team includes:");
        assertThat(content).contains("alice");
        assertThat(content).contains("carol");
        assertThat(content).doesNotContain("\"bob\"");
    }

    @Test void supervisorSeesSubordinates() {
        var registry = new InMemoryOrgRegistry();

        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "lead")
                .member("bob", "dev")
                .member("carol", "dev")
                .add()
            .supervises("alice", "bob").add()
            .supervises("alice", "carol").add()
            .build();
        org.registerAll(registry);

        @SuppressWarnings("unchecked")
        Instance<OrgRegistry> present = mock(Instance.class);
        when(present.isResolvable()).thenReturn(true);
        when(present.get()).thenReturn(registry);

        var contributor = new OrgPromptContributor(present);
        var descriptor = AgentDescriptor.builder()
                .agentId("alice").name("Alice").slot("lead").tenancyId("acme").build();
        var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        var blocks = contributor.contribute(descriptor, context);
        assertThat(blocks).hasSize(1);
        var content = blocks.getFirst().content();
        assertThat(content).contains("You can delegate work to: bob, carol");
        assertThat(content).doesNotContain("Your supervisor");
    }
}
