package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.CLAUDE_MD;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultReactiveSystemPromptRendererStreamingTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String VALID_ENRICHMENT_JSON = """
            {"identityNarrative":"You are a code reviewer.",
             "roleNarrative":"Your role is to review code.",
             "capabilityNarrative":"You can review Java and Rust code.",
             "dispositionNarrative":"You operate independently.",
             "constraintNarrative":"",
             "goalNarrative":""}""";

    EidosRenderPipeline pipeline;
    SystemPromptRenderer blockingDelegate;

    @BeforeEach
    void setUp() {
        pipeline = new EidosRenderPipeline(new CdiVocabularyRegistry(), new NoOpRenderedPromptCache(), MAPPER);
        blockingDelegate = (descriptor, context) ->
            new RenderedPrompt("blocking:" + descriptor.name(), context.format(), "dh", "ch");
    }

    static AgentDescriptor descriptor() {
        return new AgentDescriptor(
            "reviewer-1", "Code Reviewer", "1.0", "anthropic",
            "claude", "claude-3-7-sonnet", null, null, null, null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of())),
            new AgentDisposition("independent", "strict", "conservative", "directed", false),
            null, null, "default"
        );
    }

    static StreamingChatModel successMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_ENRICHMENT_JSON)).build());
            }
        };
    }

    static StreamingChatModel errorMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onError(new RuntimeException("model unavailable"));
            }
        };
    }

    @Test
    void renders_with_streaming_llm_when_present() {
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                successMock(), blockingDelegate, pipeline, MAPPER);
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.content()).isNotBlank();
        assertThat(result.format()).isEqualTo(CLAUDE_MD);
    }

    @Test
    void uses_streaming_api_not_blocking_overload() {
        final boolean[] streamingCalled = {false};
        final StreamingChatModel trackingMock = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                streamingCalled[0] = true;
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_ENRICHMENT_JSON)).build());
            }
        };
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                trackingMock, blockingDelegate, pipeline, MAPPER);

        renderer.render(descriptor(), AgentPromptContext.forFormat(CLAUDE_MD)).await().indefinitely();

        assertThat(streamingCalled[0]).isTrue();
    }

    @Test
    void falls_back_to_structural_when_streaming_llm_on_error() {
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                errorMock(), blockingDelegate, pipeline, MAPPER);
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result.content()).contains("Code Reviewer");
        assertThat(result.content()).doesNotContain("You are a code reviewer.");
    }

    @Test
    void falls_back_to_blocking_delegate_when_streaming_llm_absent() {
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                (StreamingChatModel) null, blockingDelegate, pipeline, MAPPER);
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result.content()).startsWith("blocking:Code Reviewer");
    }

    @Test
    void cache_hit_returns_without_any_llm_call() {
        final var cachingCache = new TestRenderedPromptCache();
        final var cachingPipeline = new EidosRenderPipeline(
                new CdiVocabularyRegistry(), cachingCache, MAPPER);

        // First render: cache miss -> LLM is called
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                successMock(), blockingDelegate, cachingPipeline, MAPPER);
        final var ctx = AgentPromptContext.forFormat(CLAUDE_MD);
        renderer.render(descriptor(), ctx).await().indefinitely();

        // Second render: cache hit -> LLM must NOT be called
        final StreamingChatModel throwingMock = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                throw new AssertionError("LLM must not be called on cache hit");
            }
        };
        final var renderer2 = new DefaultReactiveSystemPromptRenderer(
                throwingMock, blockingDelegate, cachingPipeline, MAPPER);
        final RenderedPrompt result = renderer2.render(descriptor(), ctx).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(cachingCache.getCount).isEqualTo(2);
        assertThat(cachingCache.putCount).isEqualTo(1);
    }
}
