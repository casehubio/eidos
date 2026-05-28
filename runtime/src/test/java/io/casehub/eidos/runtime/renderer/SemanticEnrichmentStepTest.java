package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
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

    static ObjectNode payload(String agentId) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("agentId", agentId);
        node.put("name", "Test Agent");
        node.put("slot", "tester");
        return node;
    }

    static ChatModel mockReturning(String json) {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(json))
                        .build();
            }
        };
    }

    static ChatModel capturingMock(String[] captured) {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                captured[0] = request.messages().stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .findFirst().orElse("");
                captured[1] = request.messages().stream()
                        .filter(m -> m instanceof dev.langchain4j.data.message.SystemMessage)
                        .map(m -> ((dev.langchain4j.data.message.SystemMessage) m).text())
                        .findFirst().orElse("");
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("""
                            {"identityNarrative":"id","roleNarrative":"role",
                             "capabilityNarrative":"cap","dispositionNarrative":"",
                             "constraintNarrative":"","goalNarrative":""}"""))
                        .build();
            }
        };
    }

    @Test
    void parse_valid_json_populates_required_fields() {
        final String json = """
            {"identityNarrative":"You are TestAgent.",
             "roleNarrative":"Your role is testing.",
             "capabilityNarrative":"You can test things.",
             "dispositionNarrative":"You are strict.",
             "constraintNarrative":"",
             "goalNarrative":""}""";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(json), payload("a1"));

        assertThat(result).isPresent();
        assertThat(result.get().identityNarrative()).isEqualTo("You are TestAgent.");
        assertThat(result.get().roleNarrative()).isEqualTo("Your role is testing.");
        assertThat(result.get().capabilityNarrative()).isEqualTo("You can test things.");
        assertThat(result.get().dispositionNarrative()).contains("You are strict.");
    }

    @Test
    void blank_optional_fields_become_empty() {
        final String json = """
            {"identityNarrative":"id","roleNarrative":"role","capabilityNarrative":"cap",
             "dispositionNarrative":"","constraintNarrative":"  ","goalNarrative":""}""";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(json), payload("a1"));

        assertThat(result.get().dispositionNarrative()).isEmpty();
        assertThat(result.get().constraintNarrative()).isEmpty();
        assertThat(result.get().goalNarrative()).isEmpty();
    }

    @Test
    void non_blank_optional_field_is_present() {
        final String json = """
            {"identityNarrative":"id","roleNarrative":"role","capabilityNarrative":"cap",
             "dispositionNarrative":"","constraintNarrative":"","goalNarrative":"Review PR #42."}""";

        final Optional<SemanticEnrichment> result = step.enrich(mockReturning(json), payload("a1"));

        assertThat(result.get().goalNarrative()).contains("Review PR #42.");
    }

    @Test
    void exception_from_llm_returns_empty() {
        final ChatModel throwing = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                throw new RuntimeException("Model unavailable");
            }
        };

        assertThat(step.enrich(throwing, payload("a1"))).isEmpty();
    }

    @Test
    void malformed_json_returns_empty() {
        assertThat(step.enrich(mockReturning("not json at all"), payload("a1"))).isEmpty();
    }

    @Test
    void user_message_contains_payload_fields() {
        final String[] captured = {"", ""};
        step.enrich(capturingMock(captured), payload("agent-42"));
        assertThat(captured[0]).contains("agent-42");
    }

    @Test
    void system_message_equals_prompt_template() {
        final String[] captured = {"", ""};
        step.enrich(capturingMock(captured), payload("x"));
        assertThat(captured[1]).isEqualTo(ClaudeMarkdownRenderer.PROMPT_TEMPLATE);
    }

    @Test
    void chat_request_has_response_format() {
        final boolean[] hasFormat = {false};
        final ChatModel checkingMock = new ChatModel() {
            @Override
            public ChatResponse doChat(final ChatRequest request) {
                hasFormat[0] = request.parameters() != null
                        && request.parameters().responseFormat() != null;
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from("""
                            {"identityNarrative":"id","roleNarrative":"r","capabilityNarrative":"c",
                             "dispositionNarrative":"","constraintNarrative":"","goalNarrative":""}"""))
                        .build();
            }
        };
        step.enrich(checkingMock, payload("x"));
        assertThat(hasFormat[0]).isTrue();
    }
}
