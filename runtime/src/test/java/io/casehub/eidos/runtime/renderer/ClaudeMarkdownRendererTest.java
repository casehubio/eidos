package io.casehub.eidos.runtime.renderer;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.CLAUDE_MD;
import static org.assertj.core.api.Assertions.*;

class ClaudeMarkdownRendererTest {

    static final String LLM_RESPONSE = "You are a code reviewer specialising in Java.";

    ChatModel mockLlm;
    ClaudeMarkdownRenderer rendererWithLlm;
    ClaudeMarkdownRenderer rendererStructural;

    @BeforeEach
    void setUp() {
        mockLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from(LLM_RESPONSE)).build();
            }
        };
        final var vocab = new CdiVocabularyRegistry();
        rendererWithLlm = new ClaudeMarkdownRenderer(mockLlm, vocab);
        rendererStructural = new ClaudeMarkdownRenderer((ChatModel) null, vocab);
    }

    static AgentDescriptor fullDescriptor() {
        return new AgentDescriptor(
            "reviewer-1", "Code Reviewer", "1.0", "anthropic",
            "claude", "claude-3-7-sonnet", null,
            null, null, null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.95, 150L, "low",
                List.of("code"), List.of("review"), List.of(),
                Map.of("java", 0.95, "rust", 0.3))),
            new AgentDisposition("independent", "strict", "conservative", "directed", false),
            "EU", "gdpr-compliant", "default"
        );
    }

    static AgentPromptContext fullContext() {
        return AgentPromptContext.forFormat(CLAUDE_MD)
                .withGoal(new GoalContext("Review PR #42", List.of("Check style", "Check tests"), "case-123"))
                .withResources(List.of(new Resource("/src/main/java", "Source", "filesystem")))
                .withSituationalContext("Critical release branch");
    }

    // --- LLM path ---

    @Test
    void llm_path_uses_llm_response_as_content() {
        final var result = rendererWithLlm.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains(LLM_RESPONSE);
    }

    @Test
    void llm_path_sends_yaml_containing_agent_id() {
        final String[] capturedInput = new String[1];
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                capturedInput[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce("", (a, b) -> a + b);
                return ChatResponse.builder().aiMessage(AiMessage.from("rendered")).build();
            }
        };
        final var renderer = new ClaudeMarkdownRenderer(capturingLlm, new CdiVocabularyRegistry());
        renderer.render(fullDescriptor(), fullContext());
        assertThat(capturedInput[0]).contains("reviewer-1");
    }

    @Test
    void llm_path_yaml_contains_capabilities() {
        final String[] capturedInput = new String[1];
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                capturedInput[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce("", (a, b) -> a + b);
                return ChatResponse.builder().aiMessage(AiMessage.from("rendered")).build();
            }
        };
        new ClaudeMarkdownRenderer(capturingLlm, new CdiVocabularyRegistry())
                .render(fullDescriptor(), fullContext());
        assertThat(capturedInput[0]).contains("code-review");
    }

    @Test
    void llm_path_yaml_contains_goal_when_set() {
        final String[] capturedInput = new String[1];
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                capturedInput[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce("", (a, b) -> a + b);
                return ChatResponse.builder().aiMessage(AiMessage.from("rendered")).build();
            }
        };
        new ClaudeMarkdownRenderer(capturingLlm, new CdiVocabularyRegistry())
                .render(fullDescriptor(), fullContext());
        assertThat(capturedInput[0]).contains("Review PR #42");
    }

    @Test
    void llm_path_yaml_contains_resources_when_set() {
        final String[] capturedInput = new String[1];
        final ChatModel capturingLlm = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                capturedInput[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce("", (a, b) -> a + b);
                return ChatResponse.builder().aiMessage(AiMessage.from("rendered")).build();
            }
        };
        new ClaudeMarkdownRenderer(capturingLlm, new CdiVocabularyRegistry())
                .render(fullDescriptor(), fullContext());
        assertThat(capturedInput[0]).contains("/src/main/java");
    }

    // --- Structural path ---

    @Test
    void structural_path_contains_agent_name_and_id() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Code Reviewer");
        assertThat(result.content()).contains("reviewer-1");
    }

    @Test
    void structural_path_contains_capability() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("code-review");
    }

    @Test
    void structural_path_contains_disposition_axes() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("independent");
        assertThat(result.content()).contains("strict");
    }

    @Test
    void structural_path_contains_goal_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Review PR #42");
    }

    @Test
    void structural_path_omits_goal_section_when_absent() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Goal");
    }

    @Test
    void structural_path_contains_resources_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("/src/main/java");
    }

    @Test
    void structural_path_omits_resources_section_when_empty() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Resources");
    }

    @Test
    void structural_path_contains_situational_context_when_set() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.content()).contains("Critical release branch");
    }

    @Test
    void structural_path_omits_context_section_when_null() {
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        final var result = rendererStructural.render(fullDescriptor(), ctx);
        assertThat(result.content()).doesNotContain("## Context");
    }

    // --- Hashing ---

    @Test
    void same_inputs_produce_same_hashes() {
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(r1.descriptorHash()).isEqualTo(r2.descriptorHash());
        assertThat(r1.contextHash()).isEqualTo(r2.contextHash());
    }

    @Test
    void different_descriptor_produces_different_descriptor_hash() {
        final var desc2 = new AgentDescriptor(
            "planner-1", "Planner", "1.0", "anthropic", "claude", "claude-3-7-sonnet",
            null, null, null, null, "planner",
            List.of(), null, null, null, "default"
        );
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(desc2, fullContext());
        assertThat(r1.descriptorHash()).isNotEqualTo(r2.descriptorHash());
    }

    @Test
    void different_context_produces_different_context_hash() {
        final var ctx2 = AgentPromptContext.forFormat(CLAUDE_MD).withSituationalContext("different");
        final var r1 = rendererStructural.render(fullDescriptor(), fullContext());
        final var r2 = rendererStructural.render(fullDescriptor(), ctx2);
        assertThat(r1.contextHash()).isNotEqualTo(r2.contextHash());
    }

    @Test
    void rendered_prompt_has_correct_format() {
        final var result = rendererStructural.render(fullDescriptor(), fullContext());
        assertThat(result.format()).isEqualTo(CLAUDE_MD);
    }
}
