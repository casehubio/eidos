package io.casehub.eidos.runtime.renderer;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.PromptBlock;
import io.casehub.eidos.api.PromptTier;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.spi.PromptContributor;
import io.casehub.eidos.core.renderer.DefaultPromptAssembler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPromptAssemblerTest {

    static AgentDescriptor minimalDescriptor() {
        return AgentDescriptor.builder()
                .agentId("test-agent").name("Test").slot("tester").tenancyId("default")
                .build();
    }

    static AgentPromptContext markdownContext() {
        return AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
    }

    @Test
    void noContributors_systemPromptOnly() {
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "rendered system prompt",
                List.of());
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.systemPrompt()).isEqualTo("rendered system prompt");
        assertThat(result.userMessage()).isEmpty();
    }

    @Test
    void cognitiveBlocks_goToUserMessage() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.cognitive("MOOD", "warmth: 0.7"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.userMessage()).contains("[MOOD]");
        assertThat(result.userMessage()).contains("warmth: 0.7");
    }

    @Test
    void commandBlocks_goToUserMessage_withDoTag() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.command("Call MCP gardenSearch"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.userMessage()).contains("[DO]");
        assertThat(result.userMessage()).contains("Call MCP gardenSearch");
    }

    @Test
    void conversationBlocks_noTag_goToUserMessage() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.conversation("Hey, how are you?"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.userMessage()).contains("Hey, how are you?");
        assertThat(result.userMessage()).doesNotContain("[");
    }

    @Test
    void tierOrdering_cognitive_beforeCommand_beforeConversation() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.conversation("chat text"),
                PromptBlock.command("do something"),
                PromptBlock.cognitive("MOOD", "happy"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        int moodIdx = result.userMessage().indexOf("[MOOD]");
        int doIdx = result.userMessage().indexOf("[DO]");
        int chatIdx = result.userMessage().indexOf("chat text");
        assertThat(moodIdx).isLessThan(doIdx);
        assertThat(doIdx).isLessThan(chatIdx);
    }

    @Test
    void identityBlocks_fromContributor_goToSystemPrompt() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.identity("cognitive brief template content"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "base system prompt", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.systemPrompt()).contains("base system prompt");
        assertThat(result.systemPrompt()).contains("cognitive brief template content");
    }

    @Test
    void salience_higherAppearsFirst_withinTier() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                new PromptBlock(PromptTier.COGNITIVE, "LOW", 0.1f, "low salience"),
                new PromptBlock(PromptTier.COGNITIVE, "HIGH", 0.9f, "high salience"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        int highIdx = result.userMessage().indexOf("[HIGH]");
        int lowIdx = result.userMessage().indexOf("[LOW]");
        assertThat(highIdx).isLessThan(lowIdx);
    }

    @Test
    void blockDelimiters_doubleNewlineBetweenBlocks() {
        PromptContributor contributor = (desc, ctx) -> List.of(
                PromptBlock.cognitive("MOOD", "happy"),
                PromptBlock.cognitive("DRIVES", "curiosity"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(contributor));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.userMessage()).contains("[MOOD]\nhappy\n\n[DRIVES]\ncuriosity");
    }

    @Test
    void contributorException_skippedGracefully() {
        PromptContributor broken = (desc, ctx) -> { throw new RuntimeException("boom"); };
        PromptContributor healthy = (desc, ctx) -> List.of(
                PromptBlock.cognitive("MOOD", "calm"));
        var assembler = new DefaultPromptAssembler(
                (desc, ctx) -> "system", List.of(broken, healthy));
        var result = assembler.assemble(minimalDescriptor(), markdownContext());
        assertThat(result.userMessage()).contains("[MOOD]");
        assertThat(result.userMessage()).contains("calm");
    }
}
