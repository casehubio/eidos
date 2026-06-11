package io.casehub.eidos.eval;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.stream.Collectors;

/**
 * Bridges LangChain4j's {@link ChatModel} to the platform's {@link AgentProvider} SPI.
 *
 * <p>Registered as {@code @DefaultBean} — active when no higher-priority {@code ChatModel}
 * bean is present. Jlama's extension registers {@code ChatModel @ApplicationScoped}, which
 * beats {@code @DefaultBean} automatically in Jlama runs.
 *
 * <p>{@code ResponseFormat} is silently discarded: Claude CLI has no structured output
 * parameter. JSON structure relies on prompt-engineered instructions in judge system prompts.
 */
@DefaultBean
@ApplicationScoped
@IfBuildProperty(name = "casehub.eval.claude-provider.enabled", stringValue = "true", enableIfMissing = true)
class AgentProviderChatModel implements ChatModel {

    private final AgentProvider fixedProvider;
    private final Instance<AgentProvider> agentProviderInstance;

    @Inject
    AgentProviderChatModel(@Any final Instance<AgentProvider> agentProviderInstance) {
        this.fixedProvider = null;
        this.agentProviderInstance = agentProviderInstance;
    }

    /** Package-private constructor for unit tests — no CDI required. */
    AgentProviderChatModel(final AgentProvider agentProvider) {
        this.fixedProvider = agentProvider;
        this.agentProviderInstance = null;
    }

    @Override
    public ChatResponse doChat(final ChatRequest request) {
        final String systemPrompt = request.messages().stream()
            .filter(m -> m instanceof SystemMessage)
            .map(m -> ((SystemMessage) m).text())
            .findFirst()
            .orElse("");
        final String userPrompt = request.messages().stream()
            .filter(m -> m instanceof UserMessage)
            .map(m -> ((UserMessage) m).singleText())
            .findFirst()
            .orElse("");

        final AgentProvider provider = fixedProvider != null
            ? fixedProvider
            : agentProviderInstance.get();

        final String text = provider.invoke(AgentSessionConfig.of(systemPrompt, userPrompt))
            .map(e -> switch (e) { case AgentEvent.TextDelta td -> td.text(); })
            .collect().asList()
            .await().indefinitely()
            .stream().collect(Collectors.joining());

        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }
}
