package io.casehub.eidos.eval;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentProviderChatModelTest {

    @Test
    void chat_routes_system_and_user_messages_to_agentProvider() {
        final AtomicReference<AgentSessionConfig> captured = new AtomicReference<>();
        final AgentProvider provider = config -> {
            captured.set(config);
            return Multi.createFrom().items(new AgentEvent.TextDelta("hello "), new AgentEvent.TextDelta("world"));
        };
        final var model = new AgentProviderChatModel(provider);

        final var request = ChatRequest.builder()
            .messages(SystemMessage.from("System: be helpful."), UserMessage.from("Question?"))
            .build();
        final ChatResponse response = model.doChat(request);

        assertThat(captured.get().systemPrompt()).isEqualTo("System: be helpful.");
        assertThat(captured.get().userPrompt()).isEqualTo("Question?");
        assertThat(response.aiMessage().text()).isEqualTo("hello world");
    }

    @Test
    void chat_concatenates_all_text_deltas() {
        final AgentProvider provider = config -> Multi.createFrom().items(
            new AgentEvent.TextDelta("A"),
            new AgentEvent.TextDelta("B"),
            new AgentEvent.TextDelta("C"));
        final var model = new AgentProviderChatModel(provider);

        final var response = model.doChat(ChatRequest.builder()
            .messages(SystemMessage.from("s"), UserMessage.from("u"))
            .build());

        assertThat(response.aiMessage().text()).isEqualTo("ABC");
    }

    @Test
    void chat_times_out_when_provider_never_completes() {
        final AgentProvider hangingProvider = config ->
            Multi.createFrom().emitter(e -> { /* never emit or complete */ });
        final var model = new AgentProviderChatModel(hangingProvider, Duration.ofMillis(50));

        assertThatThrownBy(() -> model.doChat(ChatRequest.builder()
                .messages(SystemMessage.from("s"), UserMessage.from("u"))
                .build()))
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    void chat_ignores_responseFormat() {
        final AgentProvider provider = config ->
            Multi.createFrom().item(new AgentEvent.TextDelta("{\"answer\":42}"));
        final var model = new AgentProviderChatModel(provider);

        // ResponseFormat can't be expressed via Claude CLI — bridge silently ignores it
        final var response = model.doChat(ChatRequest.builder()
            .messages(SystemMessage.from("s"), UserMessage.from("u"))
            .build());

        assertThat(response.aiMessage().text()).isEqualTo("{\"answer\":42}");
    }
}
