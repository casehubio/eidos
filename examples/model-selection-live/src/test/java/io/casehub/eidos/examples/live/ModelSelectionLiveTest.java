package io.casehub.eidos.examples.live;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live model selection demo — invokes Claude via the platform AgentProvider.
 *
 * <p>Run with:
 * <pre>
 * JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean test \
 *   -pl examples/model-selection-live -Dcasehub.live=true
 * </pre>
 *
 * <p>Requires {@code claude} CLI installed and authenticated.
 *
 * <p>Flow:
 * <ol>
 *   <li>Eidos descriptor declares modelTier=flagship on deep-analysis capability</li>
 *   <li>AgentSelector picks the agent based on capability match</li>
 *   <li>SystemPromptRenderer generates the system prompt (MARKDOWN format)</li>
 *   <li>AgentProvider.invoke() streams the response from Claude</li>
 * </ol>
 */
@QuarkusTest
@TestProfile(LiveDemoProfile.class)
class ModelSelectionLiveTest {

    static final String TENANCY = "live-demo";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentSelector selector;
    @Inject AgentProvider agentProvider;

    @Test
    void full_flow_descriptor_to_llm_response() {
        // 1. Look up the agent
        var desc = registry.findById("flagship-analyst", TENANCY).orElseThrow();
        var cap = desc.capabilities().stream()
            .filter(c -> c.name().equals("deep-analysis")).findFirst().orElseThrow();

        System.out.println("═══ Model Selection Live Demo ═══");
        System.out.println();
        System.out.println("Agent:             " + desc.name());
        System.out.println("Capability:        " + cap.name());
        System.out.println("Model tier:        " + cap.modelTier());
        System.out.println("Model capabilities:" + cap.modelCapabilities());
        System.out.println("Quality hint:      " + cap.qualityHint());
        System.out.println();

        // 2. Render the system prompt
        var systemPrompt = renderer.render(desc,
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
        System.out.println("── System Prompt (first 300 chars) ──");
        System.out.println(systemPrompt.content().substring(0,
            Math.min(300, systemPrompt.content().length())));
        System.out.println("...");
        System.out.println();

        // 3. Invoke Claude via AgentProvider
        System.out.println("── Invoking Claude via AgentProvider ──");
        String response = agentProvider.invoke(
                AgentSessionConfig.of(
                    systemPrompt.content(),
                    "Describe yourself: what kind of agent are you, what are your "
                    + "capabilities, and what model tiers do they require? "
                    + "Keep it under 4 sentences.",
                    Duration.ofSeconds(30)))
            .filter(e -> e instanceof AgentEvent.TextDelta)
            .map(e -> ((AgentEvent.TextDelta) e).text())
            .collect().with(Collectors.joining())
            .await().atMost(Duration.ofSeconds(60));

        System.out.println();
        System.out.println("── Response ──");
        System.out.println(response);
        System.out.println();
        System.out.println("═══ Demo Complete ═══");

        assertThat(response).isNotBlank();
    }

    @Test
    void agent_selection_then_invoke() {
        // 1. Find candidates by capability
        var candidates = registry.find(
            AgentQuery.byCapability("deep-analysis", TENANCY));
        assertThat(candidates).isNotEmpty();

        // 2. Select best agent
        var context = SelectionContext.of(TENANCY, "deep-analysis");
        var selection = selector.select(candidates, context);
        assertThat(selection).isInstanceOf(AgentSelection.Selected.class);
        var selected = (AgentSelection.Selected) selection;

        System.out.println("═══ Agent Selection → Invocation ═══");
        System.out.println("Selected: " + selected.agent().name());
        System.out.println("Score:    " + selected.trustScore());

        // 3. Render and invoke
        var prompt = renderer.render(selected.agent(),
            AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

        String response = agentProvider.invoke(
                AgentSessionConfig.of(prompt.content(),
                    "What is the capital of France? One word.",
                    Duration.ofSeconds(15)))
            .filter(e -> e instanceof AgentEvent.TextDelta)
            .map(e -> ((AgentEvent.TextDelta) e).text())
            .collect().with(Collectors.joining())
            .await().atMost(Duration.ofSeconds(30));

        System.out.println("Response: " + response);
        System.out.println("═══ Done ═══");
        assertThat(response.toLowerCase()).contains("paris");
    }
}
