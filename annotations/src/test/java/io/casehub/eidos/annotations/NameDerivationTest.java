package io.casehub.eidos.annotations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class NameDerivationTest {

    @ParameterizedTest
    @CsvSource({
        "LegalAnalystAgent, legal-analyst-agent",
        "Reviewer, reviewer",
        "DocumentAnalyst, document-analyst",
        "HTMLParser, html-parser",
        "APIGatewayAgent, api-gateway-agent",
        "NLPAnalyser, nlp-analyser",
        "A, a"
    })
    void toKebabCase(String input, String expected) {
        assertThat(NameDerivation.toKebabCase(input)).isEqualTo(expected);
    }

    @Test
    void toKebabCaseInnerClass() {
        assertThat(NameDerivation.toKebabCase("OuterClass$InnerAgent")).isEqualTo("inner-agent");
    }

    @ParameterizedTest
    @CsvSource({
        "LegalAnalystAgent, Legal Analyst Agent",
        "Reviewer, Reviewer",
        "DocumentAnalyst, Document Analyst"
    })
    void toDisplayName(String input, String expected) {
        assertThat(NameDerivation.toDisplayName(input)).isEqualTo(expected);
    }

    @Test
    void toDisplayNameInnerClass() {
        assertThat(NameDerivation.toDisplayName("OuterClass$InnerAgent")).isEqualTo("Inner Agent");
    }

    @Test
    void emptyAndNull() {
        assertThat(NameDerivation.toKebabCase("")).isEmpty();
        assertThat(NameDerivation.toKebabCase(null)).isEmpty();
        assertThat(NameDerivation.toDisplayName("")).isEmpty();
        assertThat(NameDerivation.toDisplayName(null)).isEmpty();
    }
}
