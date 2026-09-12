package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.eval.FunctionActivationJudge.FunctionActivationResult;
import io.casehub.eidos.eval.FunctionActivationJudge.FunctionScenario;
import io.casehub.eidos.runtime.registrar.DescriptorCollector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(EvalProfile.class)
@Tag("eval")
class MinimalBriefingEvalTest {

    static List<JungianProfile> profiles;
    static Map<String, List<FunctionScenario>> scenariosByFunction;

    @BeforeAll
    static void loadData() {
        profiles = new JungianProfileLoader().load();
        scenariosByFunction = FunctionScenarioLoader.load();
    }

    @Inject SystemPromptRenderer renderer;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject FunctionActivationJudge functionJudge;
    @Inject ObjectMapper mapper;

    @ConfigProperty(name = "casehub.eval.vertex.judge-model", defaultValue = "")
    String judgeModelName;

    @ConfigProperty(name = "casehub.eval.vertex.project-id", defaultValue = "")
    String vertexProjectId;

    @ConfigProperty(name = "casehub.eval.vertex.region", defaultValue = "us-east5")
    String vertexRegion;

    @ConfigProperty(name = "casehub.eval.model.label", defaultValue = "claude")
    String modelLabel;

    @Test
    void compareBriefingContribution() throws Exception {
        final List<BriefingExperimentReport.ProfileResult> profileResults = new ArrayList<>();
        final long experimentStart = System.currentTimeMillis();

        if (!judgeModelName.isEmpty() && !vertexProjectId.isEmpty()) {
            final var judgeModel = new VertexChatModel(vertexProjectId, vertexRegion,
                    judgeModelName, java.time.Duration.ofSeconds(120));
            functionJudge.setJudgeModel(judgeModel);
            System.out.println("Judge model: " + judgeModelName + " (separate from agent)");
        } else {
            System.out.println("Judge model: same as agent model");
        }

        System.out.printf("%n=== Minimal Briefing Experiment — %d profiles × %d conditions ===%n%n",
                profiles.size(), BriefingCondition.values().length);

        for (int p = 0; p < profiles.size(); p++) {
            final JungianProfile profile = profiles.get(p);
            final List<FunctionScenario> scenarios = Stream.concat(
                scenariosByFunction.getOrDefault(profile.dominantFunction(), List.of()).stream(),
                scenariosByFunction.getOrDefault(profile.auxiliaryFunction(), List.of()).stream()
            ).toList();

            final long profileStart = System.currentTimeMillis();
            System.out.printf("[profile %d/%d] %s (%s) — dom=%s, aux=%s%n",
                    p + 1, profiles.size(), profile.name(), profile.mbtiType(),
                    profile.dominantFunction(), profile.auxiliaryFunction());

            final Map<BriefingCondition, FunctionActivationResult> conditions = new EnumMap<>(BriefingCondition.class);
            for (final BriefingCondition condition : BriefingCondition.values()) {
                final long condStart = System.currentTimeMillis();
                final AgentDescriptor descriptor = condition.apply(profile);
                final AgentDescriptor derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
                final String prompt = renderer.render(derived,
                    AgentPromptContext.forFormat(RenderFormat.MARKDOWN)).content();
                final FunctionActivationResult result = functionJudge.evaluate(
                    prompt, profile.mbtiType(), scenarios);
                conditions.put(condition, result);
                System.out.printf("  [%s] %s TAA=%.2f (%d/%d) — %dms%n",
                        profile.name(), condition, result.taa(),
                        result.correctActivations(), result.scenarioCount(),
                        System.currentTimeMillis() - condStart);
            }
            System.out.printf("  [%s] profile done — %dms%n", profile.name(),
                    System.currentTimeMillis() - profileStart);

            profileResults.add(new BriefingExperimentReport.ProfileResult(
                profile.name(), profile.mbtiType(),
                profile.dominantFunction(), profile.auxiliaryFunction(),
                conditions));
        }

        final Map<BriefingCondition, BriefingExperimentReport.ConditionSummary> aggregated =
            new EnumMap<>(BriefingCondition.class);
        for (final BriefingCondition c : BriefingCondition.values()) {
            final double meanTaa = profileResults.stream()
                .mapToDouble(p -> p.conditions().get(c).taa())
                .average().orElse(0.0);
            aggregated.put(c, new BriefingExperimentReport.ConditionSummary(meanTaa));
        }

        final double frameworkContribution =
            aggregated.get(BriefingCondition.JUNGIAN_MINIMAL).meanTaa()
            - aggregated.get(BriefingCondition.BASELINE_MINIMAL).meanTaa();
        final double briefingContribution =
            aggregated.get(BriefingCondition.BASELINE_RICH).meanTaa()
            - aggregated.get(BriefingCondition.BASELINE_MINIMAL).meanTaa();

        final BriefingExperimentReport report = new BriefingExperimentReport(
            Instant.now(), modelLabel, profileResults,
            aggregated, frameworkContribution, briefingContribution);

        Files.createDirectories(Path.of("target"));
        mapper.copy()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writeValue(Path.of("target/briefing-experiment-report.json").toFile(), report);

        System.out.println(summaryTable(report));
        System.out.printf("Total experiment time: %ds%n", (System.currentTimeMillis() - experimentStart) / 1000);

        assertThat(aggregated.get(BriefingCondition.JUNGIAN_RICH).meanTaa())
            .as("JUNGIAN_RICH mean TAA should exceed BASELINE_MINIMAL")
            .isGreaterThanOrEqualTo(aggregated.get(BriefingCondition.BASELINE_MINIMAL).meanTaa());
    }

    static String summaryTable(BriefingExperimentReport report) {
        final var sb = new StringBuilder();
        sb.append("\n=== Minimal Briefing Experiment ===\n\n");
        sb.append(String.format("%-20s | %8s | %9s | %8s | %9s%n",
            "Profile", "Base+Min", "Base+Rich", "Jung+Min", "Jung+Rich"));
        sb.append(String.format("%-20s-+----------+-----------+----------+-----------%n", "-".repeat(20)));
        for (final var p : report.profiles()) {
            sb.append(String.format("%-20s | %8.2f | %9.2f | %8.2f | %9.2f%n",
                p.name(),
                p.conditions().get(BriefingCondition.BASELINE_MINIMAL).taa(),
                p.conditions().get(BriefingCondition.BASELINE_RICH).taa(),
                p.conditions().get(BriefingCondition.JUNGIAN_MINIMAL).taa(),
                p.conditions().get(BriefingCondition.JUNGIAN_RICH).taa()));
        }
        sb.append(String.format("%-20s-+----------+-----------+----------+-----------%n", "-".repeat(20)));
        sb.append(String.format("%-20s | %8.2f | %9.2f | %8.2f | %9.2f%n",
            "Mean",
            report.aggregated().get(BriefingCondition.BASELINE_MINIMAL).meanTaa(),
            report.aggregated().get(BriefingCondition.BASELINE_RICH).meanTaa(),
            report.aggregated().get(BriefingCondition.JUNGIAN_MINIMAL).meanTaa(),
            report.aggregated().get(BriefingCondition.JUNGIAN_RICH).meanTaa()));
        sb.append(String.format("%nFramework contribution (Jung+Min - Base+Min): %+.2f%n", report.frameworkContribution()));
        sb.append(String.format("Briefing contribution (Base+Rich - Base+Min): %+.2f%n", report.briefingContribution()));
        return sb.toString();
    }
}
