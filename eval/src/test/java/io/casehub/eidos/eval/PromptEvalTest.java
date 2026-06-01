package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline quality evaluation harness. Excluded from CI via @Tag("eval").
 *
 * To run: JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval -Peval -Dgroups=eval
 *
 * Requires a ChatModel CDI bean — add a LangChain4j provider to eval/pom.xml
 * and configure credentials (see application-eval.properties template).
 */
@QuarkusTest
@TestProfile(EvalProfile.class)
@Tag("eval")
class PromptEvalTest {

    private static final Map<RenderFormat, Double> SCORE_FLOORS = Map.of(
        RenderFormat.MARKDOWN,  3.5,
        RenderFormat.PROSE,     3.5,
        RenderFormat.A2A_CARD,  3.5
    );

    private static final double PROXIMITY_FLOOR = 3.0;

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
        final List<String> reliabilityWarnings = runReliabilityCheck(sample, renders, variantIndex);

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

    private List<String> runReliabilityCheck(
        final List<ProfiledEvalCase> sample,
        final Map<ProfiledEvalCase, RenderedPrompt> renders,
        final VariantIndex index
    ) throws Exception {
        final List<String> warnings = new ArrayList<>();
        for (final ProfiledEvalCase c : sample) {
            final TraitExpressionResult r1 = traitExpressionJudge.evaluate(c, renders.get(c));
            final TraitExpressionResult r2 = traitExpressionJudge.evaluate(c, renders.get(c));
            for (final String axis : List.of("socialOrient", "ruleFollowing", "riskAppetite", "autonomy")) {
                final int s1 = r1.expressionScores().getOrDefault(axis, 3);
                final int s2 = r2.expressionScores().getOrDefault(axis, 3);
                if (Math.abs(s1 - s2) >= 1) {
                    warnings.add("Stage2 variance >= 1 for " + c.profile().name() + "/" + axis
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
