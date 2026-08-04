package io.casehub.eidos.eval;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * CDI producer for {@link VertexChatModel}. Active when
 * {@code casehub.eval.vertex.enabled=true} (build-time property).
 *
 * <p>Reads project/region from config (env-var backed):
 * <ul>
 *   <li>{@code ANTHROPIC_VERTEX_PROJECT_ID} → project</li>
 *   <li>{@code CLOUD_ML_REGION} → region</li>
 * </ul>
 *
 * <p>Produces {@code @ApplicationScoped ChatModel} which beats the
 * {@code @DefaultBean AgentProviderChatModel} automatically.
 */
@ApplicationScoped
@IfBuildProperty(name = "casehub.eval.vertex.enabled", stringValue = "true")
class VertexChatModelProducer {

    private static final Logger LOG = Logger.getLogger(VertexChatModelProducer.class.getName());

    @Produces
    @ApplicationScoped
    ChatModel vertexChatModel(
            @ConfigProperty(name = "casehub.eval.vertex.project-id") final String projectId,
            @ConfigProperty(name = "casehub.eval.vertex.region", defaultValue = "us-east5") final String region,
            @ConfigProperty(name = "casehub.eval.vertex.model", defaultValue = "claude-sonnet-4-5") final String model,
            @ConfigProperty(name = "casehub.eval.vertex.timeout-seconds", defaultValue = "120") final int timeoutSeconds) {
        LOG.info(String.format("Creating VertexChatModel (agent): project=%s, region=%s, model=%s, timeout=%ds",
                               projectId, region, model, timeoutSeconds));
        return new VertexChatModel(projectId, region, model, Duration.ofSeconds(timeoutSeconds));
    }

    // Judge model is wired directly in MinimalBriefingEvalTest via setJudgeModel()
    // when casehub.eval.vertex.judge-model is set. No CDI qualifier needed.
}
