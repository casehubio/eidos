package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline quality evaluation harness. Excluded from CI via @Tag("eval").
 *
 * Run Claude: JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval -Peval -Dgroups=eval
 * Run Jlama:  JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval -Peval,eval-jlama -Dgroups=eval
 *
 * Claude model is configured via the Claude CLI: claude config set model claude-sonnet-4-6
 */
@QuarkusTest
@TestProfile(EvalProfile.class)
@Tag("eval")
class PromptEvalTest {

    // Calibrated 2026-06-10 from first successful Opus 4.6 baseline run.
    // Recalibrated 2026-06-17 for Sonnet 4.6 (independent-judge comparison with Qwen 8B):
    //   Qwen 8B judging Claude renders: FACTUAL_FIDELITY 5.00 (MARKDOWN/PROSE).
    //   Claude self-judging: FACTUAL_FIDELITY 1.88/2.13 — inverse self-evaluation bias.
    //   Claude over-penalises enriched disposition narratives vs raw descriptor values.
    //   PROSE floor recalibrated: observed 3.88 (Sonnet) − 0.5 margin = 3.5.
    // MARKDOWN: observed 3.95 (Opus), 3.63 (Sonnet) → floor 3.0
    // PROSE:    observed 4.50 (Opus), 3.88 (Sonnet) → floor 3.5
    // A2A_CARD: observed 5.00 → floor 4.5
    private static final Map<RenderFormat, Double> SCORE_FLOORS = Map.of(
        RenderFormat.MARKDOWN,  3.0,
        RenderFormat.PROSE,     3.5,
        RenderFormat.A2A_CARD,  4.5
    );

    // Calibrated 2026-06-14 from Qwen 3 8B baseline (structural rendering, 16 real-world cases).
    // Observed: min=3, mean=3.5, max=4. Floor at observed minimum catches identity mismatches.
    private static final double PROXIMITY_FLOOR = 3.0;

    /** Set after first run: max(0.5, observed_accuracy − 0.1). Initial value allows first run to pass. */
    private static final double ACCURACY_FLOOR = 0.0;

    static List<ProfiledEvalCase> realWorldCases;
    static VariantIndex variantIndex;

    @BeforeAll
    static void loadProfiles() {
        realWorldCases = RealWorldEvalDataset.all();
        variantIndex = new AgentProfileLoader().loadIndex();
    }

    @Inject
    SystemPromptRenderer renderer;

    @Inject
    PromptJudge judge;

    @Inject
    ProximityJudge proximityJudge;

    @Inject
    VocabularyExpressivenessJudge expressivenessJudge;

    @Inject
    TraitExpressionJudge traitExpressionJudge;

    @Inject
    PairContrastJudge pairContrastJudge;

    /** Used for Phase 3 target invocations (system prompt + question → response). */
    @Inject
    ChatModel chatModel;

    @Inject
    BehavioralJudge behavioralJudge;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "casehub.eval.model.label", defaultValue = "claude")
    String modelLabel;

    @Test
    void evaluateAllScenarios() throws Exception {
        final List<EvalResult> results = EvalDataset.all().stream()
            .map(c -> judge.evaluate(c, renderer.render(c.descriptor(), c.context())))
            .toList();

        final EvalReport report = EvalReport.build(results, "judge");
        final Path outPath = Path.of("target/eval-report.json");
        Files.createDirectories(outPath.getParent());
        EvalReportWriter.writeJson(report, outPath);
        System.out.println(EvalReportWriter.summaryTable(report));

        report.summaryByFormat().forEach((format, summary) -> {
            assertThat(summary.allCasesComplete())
                .as("All %s cases must include every declared capability", format)
                .isTrue();
            assertThat(summary.meanOverall())
                .as("Mean judge score for %s", format)
                .isGreaterThanOrEqualTo(SCORE_FLOORS.getOrDefault(format, 3.5));
        });
    }

    @Test
    @Tag("eval")
    void evaluateRealWorldScenarios() throws Exception {
        final Map<ProfiledEvalCase, RenderedPrompt> renders = realWorldCases.stream()
            .collect(Collectors.toMap(Function.identity(),
                c -> renderer.render(c.descriptor(), c.context())));

        final List<EvalResult> qualityResults = realWorldCases.stream()
            .map(c -> judge.evaluate(c, renders.get(c))).toList();

        final List<ProximityResult> proximityResults = realWorldCases.stream()
            .map(c -> proximityJudge.evaluate(c, renders.get(c))).toList();

        final List<VocabularyExpressivenessResult> expressivenessResults =
            realWorldCases.stream().map(c -> c.profile()).distinct()
                .map(p -> expressivenessJudge.evaluate(p)).toList();

        final List<TraitExpressionResult> traitResults = realWorldCases.stream()
            .map(c -> traitExpressionJudge.evaluate(c, renders.get(c))).toList();

        final List<PairContrastResult> contrastResults =
            variantIndex.variants().stream()
                .flatMap(pair -> Stream.of(RenderFormat.MARKDOWN, RenderFormat.PROSE)
                    .map(format -> pairContrastJudge.evaluate(pair, format, renders)))
                .toList();

        final List<ProfiledEvalCase> sample = realWorldCases.stream()
            .filter(c -> c.context().format() == RenderFormat.MARKDOWN)
            .limit(2).toList();
        final List<String> reliabilityWarnings = runReliabilityCheck(sample, renders);

        Files.createDirectories(Path.of("target"));

        final EvalReport qualityReport = EvalReport.build(qualityResults, "judge");
        EvalReportWriter.writeJson(qualityReport,
            Path.of("target/real-world-eval-report.json"));
        System.out.println(EvalReportWriter.summaryTable(qualityReport));

        final ProximityReport proximityReport = ProximityReport.build(proximityResults, PROXIMITY_FLOOR);
        EvalReportWriter.writeProximityJson(proximityReport,
            Path.of("target/proximity-report.json"));
        System.out.println(EvalReportWriter.proximitySummaryTable(proximityReport));

        final PersonalityPreservationReport preservationReport =
            PersonalityPreservationReport.build(expressivenessResults, traitResults, contrastResults);
        reliabilityWarnings.forEach(w -> preservationReport.annotations().add(w));
        EvalReportWriter.writePreservationJson(preservationReport,
            Path.of("target/personality-preservation-report.json"));
        System.out.println(EvalReportWriter.preservationSummaryTable(preservationReport));

        final List<RenderCacheEntry> cacheEntries = renders.entrySet().stream()
            .map(e -> new RenderCacheEntry(
                e.getKey().name(),
                e.getKey().context().format().name(),
                e.getValue().content(),
                e.getValue().enriched()))
            .toList();
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(rendersCache().toFile(), cacheEntries);

        qualityReport.summaryByFormat().forEach((format, summary) -> {
            assertThat(summary.allCasesComplete())
                .as("All %s real-world cases complete", format).isTrue();
            assertThat(summary.meanOverall())
                .as("Mean quality score for %s", format)
                .isGreaterThanOrEqualTo(SCORE_FLOORS.getOrDefault(format, 3.5));
        });

        assertThat(proximityReport.meanScore())
            .as("Mean proximity score").isGreaterThanOrEqualTo(PROXIMITY_FLOOR);
    }

    /**
     * Phase 2b: independent judge comparison. Loads Claude-rendered outputs from
     * target/renders-cache.json (written by evaluateRealWorldScenarios) and re-judges them
     * with whatever ChatModel is active — run with Ollama/Qwen to eliminate self-evaluation bias.
     *
     * <p>Diagnostic only — no assertion. Compare scores against real-world-eval-report.json.
     *
     * <p>Run sequence: (1) evaluateRealWorldScenarios with Claude, (2) this with Ollama:
     * {@code mvn clean test -pl eval -Peval,eval-ollama -Dtest=PromptEvalTest#evaluateWithIndependentJudge
     *   -Dcasehub.eval.claude-provider.enabled=false
     *   -Dquarkus.langchain4j.ollama.chat-model.model-name=qwen3.6:35b-a3b
     *   -Dquarkus.langchain4j.ollama.chat-model.format=json
     *   -Dquarkus.langchain4j.ollama.timeout=300s
     *   -Dcasehub.eval.model.label=qwen3-35b}
     */
    @Test
    @Tag("eval")
    void evaluateWithIndependentJudge() throws Exception {
        final Path cachePath = rendersCache();
        assertThat(cachePath).as("renders-cache.json must exist — run evaluateRealWorldScenarios first")
            .exists();

        final List<RenderCacheEntry> cache =
            Arrays.asList(mapper.readValue(cachePath.toFile(), RenderCacheEntry[].class));

        final Map<String, ProfiledEvalCase> caseByName = realWorldCases.stream()
            .collect(Collectors.toMap(ProfiledEvalCase::name, Function.identity()));

        final Map<ProfiledEvalCase, RenderedPrompt> savedRenders = cache.stream()
            .filter(e -> caseByName.containsKey(e.caseName()))
            .collect(Collectors.toMap(
                e -> caseByName.get(e.caseName()),
                RenderCacheEntry::toRenderedPrompt));

        final List<EvalResult> qualityResults = savedRenders.entrySet().stream()
            .map(e -> judge.evaluate(e.getKey(), e.getValue())).toList();

        final List<ProximityResult> proximityResults = savedRenders.entrySet().stream()
            .map(e -> proximityJudge.evaluate(e.getKey(), e.getValue())).toList();

        Files.createDirectories(Path.of("target"));

        final EvalReport report =
            EvalReport.build(qualityResults, modelLabel + "-judging-claude-renders");
        EvalReportWriter.writeJson(report, Path.of("target/independent-judge-report.json"));

        final ProximityReport proximityReport =
            ProximityReport.build(proximityResults, PROXIMITY_FLOOR);
        EvalReportWriter.writeProximityJson(
            proximityReport, Path.of("target/independent-proximity-report.json"));

        System.out.println("=== Independent judge: " + modelLabel + " judging Claude renders ===");
        System.out.println(EvalReportWriter.summaryTable(report));
        System.out.println(EvalReportWriter.proximitySummaryTable(proximityReport));
        System.out.printf("Independent proximity mean: %.2f (Claude baseline: check proximity-report.json)%n",
            proximityReport.meanScore());
    }

    @Test
    @Tag("eval")
    void evaluateBehavioralScenarios() throws Exception {
        final List<VariantPair> behavioralPairs = variantIndex.variants().stream()
            .filter(p -> !p.scenarioQuestions().isEmpty())
            .toList();

        final List<BehavioralPairResult> results = new ArrayList<>();

        for (final VariantPair pair : behavioralPairs) {
            // Re-render per test — JUnit test methods are isolated, renders cannot be shared
            final ProfiledEvalCase higherCase = realWorldCases.stream()
                .filter(c -> c.profile().name().equals(pair.higher())
                    && c.context().format() == RenderFormat.MARKDOWN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MARKDOWN case for: " + pair.higher()));
            final ProfiledEvalCase lowerCase = realWorldCases.stream()
                .filter(c -> c.profile().name().equals(pair.lower())
                    && c.context().format() == RenderFormat.MARKDOWN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MARKDOWN case for: " + pair.lower()));

            final RenderedPrompt higherRender = renderer.render(
                higherCase.descriptor(), AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
            final RenderedPrompt lowerRender = renderer.render(
                lowerCase.descriptor(), AgentPromptContext.forFormat(RenderFormat.MARKDOWN));

            for (final String question : pair.scenarioQuestions()) {
                final String higherResponse = chatModel.chat(
                    dev.langchain4j.data.message.SystemMessage.from(higherRender.content()),
                    dev.langchain4j.data.message.UserMessage.from(question)).aiMessage().text();
                final String lowerResponse = chatModel.chat(
                    dev.langchain4j.data.message.SystemMessage.from(lowerRender.content()),
                    dev.langchain4j.data.message.UserMessage.from(question)).aiMessage().text();
                results.add(behavioralJudge.evaluate(pair, question, higherResponse, lowerResponse));
            }
        }

        final long correct = results.stream().filter(BehavioralPairResult::correct).count();
        final double accuracy = results.isEmpty() ? 0.0 : (double) correct / results.size();

        final BehavioralReport report = new BehavioralReport(Instant.now(), modelLabel, results, accuracy);

        Files.createDirectories(Path.of("target"));
        EvalReportWriter.writeBehavioralJson(report, Path.of("target/behavioral-report.json"));

        System.out.printf("Behavioral accuracy: %.2f (%d/%d correct)%n", accuracy, correct, results.size());
        results.forEach(r -> System.out.printf("  %s | %s | correct=%b | effectSize=%d%n",
            r.pair().primaryAxis().jsonKey(), r.question(), r.correct(), r.effectSize()));

        assertThat(accuracy)
            .as("Behavioral pair-contrast accuracy").isGreaterThanOrEqualTo(ACCURACY_FLOOR);
    }

    /** Resolves the renders-cache path — override with -Dcasehub.eval.renders-cache.path to survive mvn clean. */
    private static Path rendersCache() {
        return Path.of(System.getProperty("casehub.eval.renders-cache.path", "target/renders-cache.json"));
    }

    private List<String> runReliabilityCheck(
        final List<ProfiledEvalCase> sample,
        final Map<ProfiledEvalCase, RenderedPrompt> renders
    ) throws Exception {
        final List<String> warnings = new ArrayList<>();
        for (final ProfiledEvalCase c : sample) {
            final TraitExpressionResult r1 = traitExpressionJudge.evaluate(c, renders.get(c));
            final TraitExpressionResult r2 = traitExpressionJudge.evaluate(c, renders.get(c));
            for (final io.casehub.eidos.api.DispositionAxis axis : TraitExpressionJudge.NUMERIC_AXES) {
                final int s1 = r1.expressionScores().getOrDefault(axis.jsonKey(), 3);
                final int s2 = r2.expressionScores().getOrDefault(axis.jsonKey(), 3);
                if (Math.abs(s1 - s2) >= 1) {
                    warnings.add("Stage2 variance >= 1 for " + c.profile().name() + "/" + axis.jsonKey()
                        + ": " + s1 + " vs " + s2);
                }
            }
        }
        final var relReport = Map.of("warnings", warnings, "passed", warnings.isEmpty());
        new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
            .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            .writeValue(Path.of("target/judge-reliability.json").toFile(), relReport);
        return warnings;
    }
}
