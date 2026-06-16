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

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultReactiveSystemPromptRendererStreamingTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String VALID_ENRICHMENT_JSON =
            "{\"dispositionNarrative\":\"You operate independently.\","
            + "\"goalNarrative\":\"\"}";

    EidosRenderPipeline pipeline;
    SystemPromptRenderer blockingDelegate;

    @BeforeEach
    void setUp() {
        pipeline = new EidosRenderPipeline(new CdiVocabularyRegistry(), MAPPER);
        blockingDelegate = (descriptor, context) ->
            new RenderedPrompt("blocking:" + descriptor.name(), context.format(), "dh", "ch");
    }

    static AgentDescriptor descriptor() {
        return AgentDescriptor.builder()
            .agentId("reviewer-1")
            .name("Code Reviewer")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slot("reviewer")
            .capabilities(List.of(new AgentCapability("code-review", 0.9, null, null,
                List.of(), List.of(), List.of(), Map.of())))
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .build())
            .tenancyId("default")
            .build();
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
                successMock(), blockingDelegate, pipeline, new TestReactiveRenderedPromptCache(), MAPPER);
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.content()).isNotBlank();
        assertThat(result.format()).isEqualTo(MARKDOWN);
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
                trackingMock, blockingDelegate, pipeline, new TestReactiveRenderedPromptCache(), MAPPER);

        renderer.render(descriptor(), AgentPromptContext.forFormat(MARKDOWN)).await().indefinitely();

        assertThat(streamingCalled[0]).isTrue();
    }

    @Test
    void falls_back_to_structural_when_streaming_llm_on_error() {
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                errorMock(), blockingDelegate, pipeline, new TestReactiveRenderedPromptCache(), MAPPER);
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result.content()).contains("Code Reviewer");
        assertThat(result.content()).doesNotContain("You are a code reviewer.");
    }

    @Test
    void falls_back_to_blocking_delegate_when_streaming_llm_absent() {
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                (StreamingChatModel) null, blockingDelegate, pipeline, new TestReactiveRenderedPromptCache(), MAPPER);
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result.content()).startsWith("blocking:Code Reviewer");
    }

    @Test
    void cache_hit_returns_without_any_llm_call() {
        // Pre-populate the cache with the key for our descriptor+context combo.
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
        final StageOneResult s1 = pipeline.buildStage1(descriptor(), ctx);
        final RenderedPrompt cachedResult = new RenderedPrompt(
            "cached-content", MARKDOWN, s1.descriptorHash(), s1.contextHash());

        final TestReactiveRenderedPromptCache prePopulated = new TestReactiveRenderedPromptCache();
        prePopulated.store.put(s1.lookupKey(), cachedResult);

        // Renderer with a throwing LLM — must not be called on cache hit
        final StreamingChatModel throwingMock = new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                throw new AssertionError("LLM must not be called on cache hit");
            }
        };
        final var renderer = new DefaultReactiveSystemPromptRenderer(
                throwingMock, blockingDelegate, pipeline, prePopulated, MAPPER);

        final RenderedPrompt result = renderer.render(descriptor(), ctx).await().indefinitely();

        assertThat(result.content()).isEqualTo("cached-content");
        assertThat(prePopulated.getCount).isEqualTo(1);
        assertThat(prePopulated.putCount).isEqualTo(0); // cache hit — no put
    }
}
