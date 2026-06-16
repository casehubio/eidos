package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticEnrichmentStepTest {

    static final ObjectMapper MAPPER = new ObjectMapper();
    SemanticEnrichmentStep step;

    @BeforeEach
    void setUp() {
        step = new SemanticEnrichmentStep(MAPPER);
    }

    static ObjectNode payload() {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "Test Agent");
        node.put("slot", "reviewer");
        final ObjectNode disp = node.putObject("disposition");
        disp.put("riskAppetite", "bold");
        disp.put("canDelegate", false);
        return node;
    }

    static ChatModel mockReturning(final String json) {
        return new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
            }
        };
    }

    @Test
    void parses_disposition_and_goal() {
        final String json = "{\"dispositionNarrative\":\"You approve boldly.\",\"goalNarrative\":\"Review PR #42.\"}";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(json), payload());

        assertThat(result).isPresent();
        assertThat(result.get().dispositionNarrative()).contains("You approve boldly.");
        assertThat(result.get().goalNarrative()).contains("Review PR #42.");
    }

    @Test
    void blank_optional_fields_become_empty() {
        final String json = "{\"dispositionNarrative\":\"\",\"goalNarrative\":\"  \"}";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(json), payload());

        assertThat(result.get().dispositionNarrative()).isEmpty();
        assertThat(result.get().goalNarrative()).isEmpty();
    }

    @Test
    void json_wrapped_in_markdown_code_block_is_parsed() {
        final String json = "{\"dispositionNarrative\":\"You approve boldly.\",\"goalNarrative\":\"\"}";
        final String wrapped = "```json\n" + json + "\n```";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(wrapped), payload());

        assertThat(result).isPresent();
        assertThat(result.get().dispositionNarrative()).contains("You approve boldly.");
    }

    @Test
    void prose_preamble_before_json_is_stripped() {
        final String response = "Here is the JSON:\n{\"dispositionNarrative\":\"Bold.\",\"goalNarrative\":\"\"}";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(response), payload());

        assertThat(result).isPresent();
        assertThat(result.get().dispositionNarrative()).contains("Bold.");
    }

    @Test
    void exception_from_llm_returns_empty() {
        final ChatModel throwing = new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                throw new RuntimeException("Model unavailable");
            }
        };
        assertThat(step.enrich(throwing, payload())).isEmpty();
    }

    @Test
    void malformed_json_retries_then_falls_back_to_empty() {
        assertThat(step.enrich(mockReturning("not json at all"), payload())).isEmpty();
    }

    @Test
    void malformed_json_causes_exactly_two_llm_calls_then_fallback() {
        final int[] callCount = {0};
        final ChatModel countingMock = new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                callCount[0]++;
                return ChatResponse.builder().aiMessage(AiMessage.from("not json")).build();
            }
        };
        final Optional<SemanticEnrichment> result = step.enrich(countingMock, payload());
        assertThat(result).isEmpty();
        assertThat(callCount[0]).isEqualTo(2); // first attempt + one retry
    }

    @Test
    void system_message_equals_prompt_template() {
        final String[] captured = {""};
        final ChatModel capturingMock = new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                r.messages().stream()
                    .filter(m -> m instanceof SystemMessage)
                    .map(m -> ((SystemMessage) m).text())
                    .findFirst().ifPresent(t -> captured[0] = t);
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"dispositionNarrative\":\"\",\"goalNarrative\":\"\"}")).build();
            }
        };
        step.enrich(capturingMock, payload());
        assertThat(captured[0]).isEqualTo(EidosRenderPipeline.PROMPT_TEMPLATE);
    }

    @Test
    void chat_request_has_response_format() {
        final boolean[] hasFormat = {false};
        final ChatModel checkingMock = new ChatModel() {
            @Override public ChatResponse doChat(final ChatRequest r) {
                hasFormat[0] = r.parameters() != null && r.parameters().responseFormat() != null;
                return ChatResponse.builder().aiMessage(AiMessage.from(
                    "{\"dispositionNarrative\":\"\",\"goalNarrative\":\"\"}")).build();
            }
        };
        step.enrich(checkingMock, payload());
        assertThat(hasFormat[0]).isTrue();
    }
}
