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

class ReactiveA2ASemanticEnrichmentStepTest {

    static final ObjectMapper MAPPER = new ObjectMapper();
    static final String VALID_A2A_JSON =
        "{\"capabilityNarratives\":[{\"name\":\"code-review\","
        + "\"description\":\"You conduct thorough Java code reviews.\"}]}";

    ReactiveA2ASemanticEnrichmentStep step;

    @BeforeEach
    void setUp() {
        step = new ReactiveA2ASemanticEnrichmentStep(MAPPER);
    }

    static ObjectNode descriptorNode() {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("agentId", "agent-1");
        node.put("name", "Test Agent");
        final var caps = node.putArray("capabilities");
        final var cap = caps.addObject();
        cap.put("name", "code-review");
        return node;
    }

    static StreamingChatModel successMock() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JSON)).build());
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
    void completes_with_a2a_enrichment_when_llm_succeeds() {
        final Optional<A2AEnrichment> result =
            step.enrich(successMock(), descriptorNode()).await().indefinitely();

        assertThat(result).isPresent();
        assertThat(result.get().capabilityNarratives()).hasSize(1);
        assertThat(result.get().capabilityNarratives().get(0).name()).isEqualTo("code-review");
        assertThat(result.get().capabilityNarratives().get(0).description())
                .contains("You conduct thorough Java code reviews.");
    }

    @Test
    void falls_back_to_empty_when_llm_fires_on_error() {
        final Optional<A2AEnrichment> result =
            step.enrich(errorMock(), descriptorNode()).await().indefinitely();

        assertThat(result).isEmpty();
    }

    @Test
    void falls_back_to_empty_when_parse_fails() {
        final StreamingChatModel malformedMock = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from("not json")).build());
            }
        };

        final Optional<A2AEnrichment> result =
            step.enrich(malformedMock, descriptorNode()).await().indefinitely();

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
                    ChatResponse.builder().aiMessage(AiMessage.from(VALID_A2A_JSON)).build());
            }
        };

        step.enrich(trackingMock, descriptorNode()).await().indefinitely();

        assertThat(streamingCalled[0]).isTrue();
    }

    @Test
    void returns_non_null_uni_for_hanging_provider() {
        final StreamingChatModel hangingMock = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {}
        };
        assertThat(step.enrich(hangingMock, descriptorNode())).isNotNull();
    }
}
