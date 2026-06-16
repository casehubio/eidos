package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveSemanticEnrichmentStepTest {

    static final ObjectMapper MAPPER = new ObjectMapper();
    static final String VALID_JSON =
        "{\"dispositionNarrative\":\"You approve boldly.\",\"goalNarrative\":\"Review PR #42.\"}";

    ReactiveSemanticEnrichmentStep step;

    @BeforeEach
    void setUp() {
        step = new ReactiveSemanticEnrichmentStep(MAPPER);
    }

    static ObjectNode payload() {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "Test Agent");
        node.put("slot", "reviewer");
        return node;
    }

    static StreamingChatModel successMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build());
            }
        };
    }

    static StreamingChatModel errorMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                handler.onError(new RuntimeException("model unavailable"));
            }
        };
    }

    static StreamingChatModel malformedJsonMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from("not valid json")).build());
            }
        };
    }

    @Test
    void completes_with_disposition_and_goal_when_llm_succeeds() {
        final Optional<SemanticEnrichment> result =
            step.enrich(successMock(), payload()).await().indefinitely();

        assertThat(result).isPresent();
        assertThat(result.get().dispositionNarrative()).contains("You approve boldly.");
        assertThat(result.get().goalNarrative()).contains("Review PR #42.");
    }

    @Test
    void falls_back_to_empty_when_llm_fires_on_error() {
        assertThat(step.enrich(errorMock(), payload()).await().indefinitely()).isEmpty();
    }

    @Test
    void falls_back_to_empty_when_parse_fails() {
        assertThat(step.enrich(malformedJsonMock(), payload()).await().indefinitely()).isEmpty();
    }

    @Test
    void invokes_streaming_api_not_blocking_overload() {
        final boolean[] streamingCalled = {false};
        final StreamingChatModel trackingMock = new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                streamingCalled[0] = true;
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build());
            }
        };
        step.enrich(trackingMock, payload()).await().indefinitely();
        assertThat(streamingCalled[0]).isTrue();
    }

    @Test
    void returns_non_null_uni_for_hanging_provider() {
        final StreamingChatModel hangingMock = new StreamingChatModel() {
            @Override
            public void doChat(final ChatRequest request, final StreamingChatResponseHandler handler) {
                // never fires — simulates hung provider
            }
        };
        assertThat(step.enrich(hangingMock, payload())).isNotNull();
    }
}
