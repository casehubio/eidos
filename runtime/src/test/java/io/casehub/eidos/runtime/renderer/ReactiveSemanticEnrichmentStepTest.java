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
    static final String VALID_JSON = """
            {"identityNarrative":"You are TestAgent.",
             "roleNarrative":"Your role is testing.",
             "capabilityNarrative":"You can test things.",
             "dispositionNarrative":"You are strict.",
             "constraintNarrative":"",
             "goalNarrative":""}""";

    ReactiveSemanticEnrichmentStep step;

    @BeforeEach
    void setUp() {
        step = new ReactiveSemanticEnrichmentStep(MAPPER);
    }

    static ObjectNode payload() {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("agentId", "agent-1");
        node.put("name", "Test Agent");
        return node;
    }

    static StreamingChatModel successMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_JSON)).build());
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

    static StreamingChatModel malformedJsonMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from("not valid json")).build());
            }
        };
    }

    @Test
    void completes_with_enrichment_when_llm_succeeds() {
        final Optional<SemanticEnrichment> result =
            step.enrich(successMock(), payload()).await().indefinitely();

        assertThat(result).isPresent();
        assertThat(result.get().identityNarrative()).isEqualTo("You are TestAgent.");
        assertThat(result.get().roleNarrative()).isEqualTo("Your role is testing.");
    }

    @Test
    void falls_back_to_empty_when_llm_fires_on_error() {
        final Optional<SemanticEnrichment> result =
            step.enrich(errorMock(), payload()).await().indefinitely();

        assertThat(result).isEmpty();
    }

    @Test
    void falls_back_to_empty_when_parse_fails() {
        final Optional<SemanticEnrichment> result =
            step.enrich(malformedJsonMock(), payload()).await().indefinitely();

        assertThat(result).isEmpty();
    }

    @Test
    void invokes_streaming_api_not_blocking_overload() {
        final boolean[] streamingCalled = {false};
        final StreamingChatModel trackingMock = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
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
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                // never fires — simulates hung provider
            }
        };
        // Just verify the Uni is returned without synchronous error.
        // Full 30s timeout test is impractical in CI.
        assertThat(step.enrich(hangingMock, payload())).isNotNull();
    }
}
