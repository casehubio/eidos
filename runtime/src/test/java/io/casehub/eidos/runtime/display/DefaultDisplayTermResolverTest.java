package io.casehub.eidos.runtime.display;

import io.casehub.eidos.api.DisplayTermResolver;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DefaultDisplayTermResolverTest {

    @Inject
    VocabularyRegistry registry;

    @Inject
    DisplayTermResolver resolver;

    @Inject
    io.casehub.platform.api.display.DisplayTermResolver platformResolver;

    @VocabularyMetadata(uri = "urn:test:devtown-roles", name = "Devtown Roles", version = "1.0")
    enum DevtownRole implements VocabularyTerm {
        PLANNER("planner", "Planner"),
        REVIEWER("reviewer", "Reviewer"),
        OBSERVER("observer", "Observer") {
            @Override public Optional<VocabularyTerm> exactMatch(Class<?> target) {
                if (target == GastownRole.class) return Optional.of(GastownRole.WITNESS);
                return Optional.empty();
            }
        };
        final String v, l;
        DevtownRole(String v, String l) { this.v = v; this.l = l; }
        @Override public String value() { return v; }
        @Override public String label() { return l; }
    }

    @VocabularyMetadata(uri = "urn:test:gastown-roles", name = "Gastown Roles", version = "1.0")
    enum GastownRole implements VocabularyTerm {
        WITNESS("witness", "Witness") {
            @Override public Optional<VocabularyTerm> exactMatch(Class<?> target) {
                if (target == DevtownRole.class) return Optional.of(DevtownRole.OBSERVER);
                return Optional.empty();
            }
        },
        POLECAT("polecat", "Polecat"),
        DEACON("deacon", "Deacon");
        final String v, l;
        GastownRole(String v, String l) { this.v = v; this.l = l; }
        @Override public String value() { return v; }
        @Override public String label() { return l; }
    }

    @VocabularyMetadata(uri = "urn:test:axis-vocab-a", name = "Axis Vocab A", version = "1.0")
    enum AxisVocabA implements VocabularyTerm {
        BOLD("bold", "Bold") {
            @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> target, DispositionAxis axis) {
                if (target == AxisVocabB.class && axis == DispositionAxis.RISK_APPETITE)
                    return Optional.of(AxisVocabB.ADVENTUROUS);
                return Optional.empty();
            }
        };
        final String v, l;
        AxisVocabA(String v, String l) { this.v = v; this.l = l; }
        @Override public String value() { return v; }
        @Override public String label() { return l; }
    }

    @VocabularyMetadata(uri = "urn:test:axis-vocab-b", name = "Axis Vocab B", version = "1.0")
    enum AxisVocabB implements VocabularyTerm {
        ADVENTUROUS("adventurous", "Adventurous");
        final String v, l;
        AxisVocabB(String v, String l) { this.v = v; this.l = l; }
        @Override public String value() { return v; }
        @Override public String label() { return l; }
    }

    @BeforeEach
    void ensureRegistered() {
        if (!registry.isRegistered("urn:test:devtown-roles"))
            registry.register(DevtownRole.class);
        if (!registry.isRegistered("urn:test:gastown-roles"))
            registry.register(GastownRole.class);
        if (!registry.isRegistered("urn:test:axis-vocab-a"))
            registry.register(AxisVocabA.class);
        if (!registry.isRegistered("urn:test:axis-vocab-b"))
            registry.register(AxisVocabB.class);
    }

    @Test
    void direct_resolution_returns_label() {
        assertThat(resolver.resolveLabel("witness", "urn:test:gastown-roles"))
            .isEqualTo("Witness");
    }

    @Test
    void direct_resolution_unknown_value_returns_raw() {
        assertThat(resolver.resolveLabel("unknown-role", "urn:test:gastown-roles"))
            .isEqualTo("unknown-role");
    }

    @Test
    void direct_resolution_unknown_vocab_returns_raw() {
        assertThat(resolver.resolveLabel("witness", "urn:nonexistent"))
            .isEqualTo("witness");
    }

    @Test
    void cross_vocab_swap_returns_target_label() {
        assertThat(resolver.resolveLabel("observer", "urn:test:devtown-roles",
            "urn:test:gastown-roles"))
            .isEqualTo("Witness");
    }

    @Test
    void cross_vocab_swap_no_match_falls_back_to_source_label() {
        assertThat(resolver.resolveLabel("planner", "urn:test:devtown-roles",
            "urn:test:gastown-roles"))
            .isEqualTo("Planner");
    }

    @Test
    void cross_vocab_swap_target_not_registered_falls_back() {
        assertThat(resolver.resolveLabel("witness", "urn:test:gastown-roles",
            "urn:nonexistent"))
            .isEqualTo("Witness");
    }

    @Test
    void axis_aware_swap_returns_target_label() {
        assertThat(resolver.resolveLabel("bold", "urn:test:axis-vocab-a",
            "urn:test:axis-vocab-b", DispositionAxis.RISK_APPETITE))
            .isEqualTo("Adventurous");
    }

    @Test
    void axis_aware_swap_wrong_axis_falls_back() {
        assertThat(resolver.resolveLabel("bold", "urn:test:axis-vocab-a",
            "urn:test:axis-vocab-b", DispositionAxis.AUTONOMY))
            .isEqualTo("Bold");
    }

    @Test
    void auto_discovery_null_source_finds_term() {
        assertThat(resolver.resolveLabel("witness", null))
            .isEqualTo("Witness");
    }

    @Test
    void auto_discovery_null_source_unknown_value_returns_raw() {
        assertThat(resolver.resolveLabel("nonexistent-term", null))
            .isEqualTo("nonexistent-term");
    }

    @Test
    void same_source_and_target_returns_source_label() {
        assertThat(resolver.resolveLabel("witness", "urn:test:gastown-roles",
            "urn:test:gastown-roles"))
            .isEqualTo("Witness");
    }

    @Test
    void null_value_returns_null() {
        assertThat(resolver.resolveLabel(null, "urn:test:gastown-roles"))
            .isNull();
    }

    // --- Platform SPI bridge tests ---

    @Test
    void mapTerm_cross_vocab_returns_target_value() {
        assertThat(platformResolver
            .mapTerm("observer", "urn:test:devtown-roles", "urn:test:gastown-roles"))
            .contains("witness");
    }

    @Test
    void mapTerm_no_match_returns_empty() {
        assertThat(platformResolver
            .mapTerm("planner", "urn:test:devtown-roles", "urn:test:gastown-roles"))
            .isEmpty();
    }

    @Test
    void mapTerm_null_value_returns_empty() {
        assertThat(platformResolver
            .mapTerm(null, "urn:test:devtown-roles", "urn:test:gastown-roles"))
            .isEmpty();
    }

    @Test
    void mapTerm_with_context_axis_aware() {
        assertThat(platformResolver
            .mapTerm("bold", "urn:test:axis-vocab-a", "urn:test:axis-vocab-b", "riskAppetite"))
            .contains("adventurous");
    }

    @Test
    void mapTerm_with_unknown_context_falls_back_to_axis_unaware() {
        assertThat(platformResolver
            .mapTerm("bold", "urn:test:axis-vocab-a", "urn:test:axis-vocab-b", "unknown"))
            .isEmpty();
    }

    @Test
    void platform_resolveLabel_matches_eidos() {
        assertThat(platformResolver.resolveLabel("witness", "urn:test:gastown-roles"))
                .isEqualTo("Witness");}
}
