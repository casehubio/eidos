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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
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
 *
 * <p>The call to {@code AgentProvider.invoke()} is bounded by {@code timeoutMinutes}
 * (default 7 min) as a second line of defence behind the platform's subprocess timeout
 * (default 5 min). A timed-out call throws {@link io.smallrye.mutiny.TimeoutException},
 * which {@code SemanticEnrichmentStep} catches and treats as a structural fallback signal.
 */
@DefaultBean
@ApplicationScoped
@IfBuildProperty(name = "casehub.eval.claude-provider.enabled", stringValue = "true", enableIfMissing = true)
class AgentProviderChatModel implements ChatModel {

    private final AgentProvider fixedProvider;
    private final Instance<AgentProvider> agentProviderInstance;
    private final Duration timeout;

    @Inject
    AgentProviderChatModel(
            @Any final Instance<AgentProvider> agentProviderInstance,
            @ConfigProperty(name = "casehub.eval.claude-provider.timeout-minutes", defaultValue = "7")
            final long timeoutMinutes) {
        this.fixedProvider = null;
        this.agentProviderInstance = agentProviderInstance;
        this.timeout = Duration.ofMinutes(timeoutMinutes);
    }

    /** Package-private constructor for unit tests — no CDI required. */
    AgentProviderChatModel(final AgentProvider agentProvider) {
        this(agentProvider, Duration.ofMinutes(7));
    }

    /** Package-private constructor for unit tests with explicit timeout. */
    AgentProviderChatModel(final AgentProvider agentProvider, final Duration timeout) {
        this.fixedProvider = agentProvider;
        this.agentProviderInstance = null;
        this.timeout = timeout;
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
            .await().atMost(timeout)
            .stream().collect(Collectors.joining());

        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }
}
