package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    private static final double SCORE_FLOOR = 3.5; // update after first baseline run

    @Inject
    SystemPromptRenderer renderer;

    @Inject
    PromptJudge judge;

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

        assertThat(report.summary().allCasesComplete())
            .as("All rendered prompts must include every declared capability name")
            .isTrue();

        assertThat(report.summary().meanOverall())
            .as("Mean judge score across all cases")
            .isGreaterThanOrEqualTo(SCORE_FLOOR);
    }
}
