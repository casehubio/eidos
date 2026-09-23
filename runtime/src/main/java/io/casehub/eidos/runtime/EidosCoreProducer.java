package io.casehub.eidos.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentGraphBackfill;
import io.casehub.eidos.api.AgentGraphQuery;
import io.casehub.eidos.api.AgentGraphStore;
import io.casehub.eidos.api.AgentStateStore;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.DispositionEvolution;
import io.casehub.eidos.api.DispositionHealth;
import io.casehub.eidos.api.DispositionProfileStore;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.GoalEvolution;
import io.casehub.eidos.api.GoalSignalStore;
import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.RuntimeCollaborationQuery;
import io.casehub.eidos.api.TaskSemanticEnricher;
import io.casehub.eidos.api.TemplateRegistry;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.casehub.eidos.api.spi.TemplateRegistrar;
import io.casehub.eidos.core.display.DefaultDisplayTermResolver;
import io.casehub.eidos.core.graph.NoOpAgentGraphBackfill;
import io.casehub.eidos.core.graph.NoOpAgentGraphQuery;
import io.casehub.eidos.core.graph.NoOpAgentGraphStore;
import io.casehub.eidos.core.graph.NoOpRuntimeCollaborationQuery;
import io.casehub.eidos.core.graph.NoOpTaskSemanticEnricher;
import io.casehub.eidos.core.health.NoOpAgentStateStore;
import io.casehub.eidos.core.health.NoOpBehavioralSignalStore;
import io.casehub.eidos.core.health.NoOpDispositionEvolution;
import io.casehub.eidos.core.health.NoOpDispositionHealth;
import io.casehub.eidos.core.health.NoOpDispositionProfileStore;
import io.casehub.eidos.core.health.NoOpDispositionSignalStore;
import io.casehub.eidos.core.health.NoOpGoalEvolution;
import io.casehub.eidos.core.health.NoOpGoalSignalStore;
import io.casehub.eidos.core.registrar.ClasspathYamlDescriptorRegistrar;
import io.casehub.eidos.core.renderer.EidosRenderPipeline;
import io.casehub.eidos.core.renderer.NoOpRenderedPromptCache;
import io.casehub.eidos.core.template.ClasspathYamlTemplateRegistrar;
import io.casehub.eidos.core.validator.BriefingCoherenceValidator;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class EidosCoreProducer {

    // ── NoOp @DefaultBean fallbacks ──────────────────────────────────────

    @Produces @DefaultBean
    AgentGraphBackfill agentGraphBackfill() {
        return new NoOpAgentGraphBackfill();
    }

    @Produces @DefaultBean
    AgentGraphQuery agentGraphQuery() {
        return new NoOpAgentGraphQuery();
    }

    @Produces @DefaultBean
    AgentGraphStore agentGraphStore() {
        return new NoOpAgentGraphStore();
    }

    @Produces @DefaultBean
    TaskSemanticEnricher taskSemanticEnricher() {
        return new NoOpTaskSemanticEnricher();
    }

    @Produces @DefaultBean
    AgentStateStore agentStateStore() {
        return new NoOpAgentStateStore();
    }

    @Produces @DefaultBean
    BehavioralSignalStore behavioralSignalStore() {
        return new NoOpBehavioralSignalStore();
    }

    @Produces @DefaultBean
    DispositionEvolution dispositionEvolution() {
        return new NoOpDispositionEvolution();
    }

    @Produces @DefaultBean
    DispositionHealth dispositionHealth() {
        return new NoOpDispositionHealth();
    }

    @Produces @DefaultBean
    DispositionSignalStore dispositionSignalStore() {
        return new NoOpDispositionSignalStore();
    }

    @Produces @DefaultBean
    DispositionProfileStore dispositionProfileStore() {
        return new NoOpDispositionProfileStore();
    }

    @Produces @DefaultBean
    GoalEvolution goalEvolution() {
        return new NoOpGoalEvolution();
    }

    @Produces @DefaultBean
    GoalSignalStore goalSignalStore() {
        return new NoOpGoalSignalStore();
    }

    @Produces @DefaultBean
    RenderedPromptCache renderedPromptCache() {
        return new NoOpRenderedPromptCache();
    }

    @Produces
    @DefaultBean
    DefaultDisplayTermResolver displayTermResolver(VocabularyRegistry registry) {
        return new DefaultDisplayTermResolver(registry);
    }

    @Produces
    @DefaultBean
    RuntimeCollaborationQuery runtimeCollaborationQuery() {
        return new NoOpRuntimeCollaborationQuery();
    }


    // ── Active @ApplicationScoped beans ──────────────────────────────────

    @Produces @ApplicationScoped
    BriefingCoherenceValidator briefingCoherenceValidator(VocabularyRegistry vocabRegistry) {
        return new BriefingCoherenceValidator(vocabRegistry);
    }

    @Produces @ApplicationScoped
    EidosRenderPipeline eidosRenderPipeline(VocabularyRegistry vocab,
                                            TemplateRegistry templateRegistry,
                                            ObjectMapper mapper) {
        return new EidosRenderPipeline(vocab, templateRegistry, mapper);
    }

    @Produces @ApplicationScoped
    AgentDescriptorRegistrar classpathYamlDescriptorRegistrar() {
        return new ClasspathYamlDescriptorRegistrar();
    }

    @Produces @ApplicationScoped
    TemplateRegistrar classpathYamlTemplateRegistrar() {
        return new ClasspathYamlTemplateRegistrar();
    }
}
