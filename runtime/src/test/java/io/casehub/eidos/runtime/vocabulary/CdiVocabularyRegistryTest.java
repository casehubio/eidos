package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class CdiVocabularyRegistryTest {

    @Inject VocabularyRegistry registry;

    @VocabularyMetadata(uri = "urn:test:source", name = "Source Vocab", version = "1.0")
    enum SourceTerm implements VocabularyTerm {
        ALPHA("alpha", "Alpha", "First term", List.of("a", "one")) {
            @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
                return t == TargetTerm.class ? Optional.of(TargetTerm.PRIMARY) : Optional.empty();
            }
        },
        BETA("beta", "Beta", "Second term", List.of("b"));

        final String value, label, description;
        final List<String> aliases;
        SourceTerm(String v, String l, String d, List<String> a) {
            value = v; label = l; description = d; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public String description()   { return description; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:target")
    enum TargetTerm implements VocabularyTerm {
        PRIMARY("primary", "Primary", List.of()),
        SECONDARY("secondary", "Secondary", List.of());

        final String value, label;
        final List<String> aliases;
        TargetTerm(String v, String l, List<String> a) {
            value = v; label = l; aliases = a;
        }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:axis-source")
    enum AxisSourceTerm implements VocabularyTerm {
        DTERM("d-type", "D Type", List.of()) {
            @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> t, DispositionAxis axis) {
                if (t != AxisTargetTerm.class) return Optional.empty();
                return switch (axis) {
                    case RISK_APPETITE      -> Optional.of(AxisTargetTerm.HIGH_RISK);
                    case SOCIAL_ORIENTATION -> Optional.empty();
                    case RULE_FOLLOWING     -> Optional.empty();
                    case AUTONOMY           -> Optional.empty();
                    case CONFLICT_MODE      -> Optional.empty();
                };
            }
        };
        final String value, label;
        final List<String> aliases;
        AxisSourceTerm(String v, String l, List<String> a) { value = v; label = l; aliases = a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:axis-target")
    enum AxisTargetTerm implements VocabularyTerm {
        HIGH_RISK("high-risk", "High Risk", List.of()),
        LOW_RISK("low-risk",  "Low Risk",  List.of());
        final String value, label;
        final List<String> aliases;
        AxisTargetTerm(String v, String l, List<String> a) { value = v; label = l; aliases = a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    // Dedicated enums for typed-bypass test — never registered anywhere in this class
    @VocabularyMetadata(uri = "urn:test:bypass-source")
    enum BypassSource implements VocabularyTerm {
        TERM("term", "Term", List.of()) {
            @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
                return t == BypassTarget.class ? Optional.of(BypassTarget.MAPPED) : Optional.empty();
            }
        };
        final String value, label;
        final List<String> aliases;
        BypassSource(String v, String l, List<String> a) { value = v; label = l; aliases = a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    @VocabularyMetadata(uri = "urn:test:bypass-target")
    enum BypassTarget implements VocabularyTerm {
        MAPPED("mapped", "Mapped", List.of());
        final String value, label;
        final List<String> aliases;
        BypassTarget(String v, String l, List<String> a) { value = v; label = l; aliases = a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }

    // --- Registration tests ---

    @Test
    void programmatic_register_and_isRegistered() {
        registry.register(SourceTerm.class);
        assertThat(registry.isRegistered("urn:test:source")).isTrue();
    }

    @Test
    void isRegistered_false_for_unknown_uri() {
        assertThat(registry.isRegistered("urn:does-not-exist")).isFalse();
    }

    @Test
    void register_missing_annotation_throws() {
        // NoMeta implements VocabularyTerm but lacks @VocabularyMetadata — registry must reject it
        enum NoMeta implements VocabularyTerm {
            X("x", "X", List.of());
            final String value, label; final List<String> aliases;
            NoMeta(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(NoMeta.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("missing @VocabularyMetadata");
    }

    @Test
    void register_zero_constant_enum_throws() {
        @VocabularyMetadata(uri = "urn:test:zero-constant")
        enum ZeroConstant implements VocabularyTerm {
            ;
            @Override public String value() { return ""; }
            @Override public String label() { return ""; }
        }
        assertThatThrownBy(() -> registry.register(ZeroConstant.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_duplicate_primary_value_throws() {
        @VocabularyMetadata(uri = "urn:test:dup-primary")
        enum DupPrimary implements VocabularyTerm {
            A("same", "A", List.of()), B("same", "B", List.of());
            final String value, label; final List<String> aliases;
            DupPrimary(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(DupPrimary.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate primary value");
    }

    @Test
    void register_alias_duplicates_primary_throws() {
        @VocabularyMetadata(uri = "urn:test:alias-collision")
        enum AliasCollision implements VocabularyTerm {
            A("alpha", "A", List.of()),
            B("beta",  "B", List.of("alpha"));
            final String value, label; final List<String> aliases;
            AliasCollision(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(AliasCollision.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("conflicts");
    }

    @Test
    void register_duplicate_uri_different_class_throws() {
        registry.register(SourceTerm.class);
        @VocabularyMetadata(uri = "urn:test:source")
        enum OtherSource implements VocabularyTerm {
            X("x", "X", List.of());
            final String value, label; final List<String> aliases;
            OtherSource(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(OtherSource.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_same_class_again_is_idempotent() {
        registry.register(SourceTerm.class);
        assertThatNoException().isThrownBy(() -> registry.register(SourceTerm.class));
    }

    // --- allTerms() tests ---

    @Test
    void allTerms_returns_distinct_constants_in_declaration_order() {
        registry.register(SourceTerm.class);
        var terms = registry.allTerms("urn:test:source");
        assertThat(terms).hasSize(2);
        assertThat(terms.get(0).value()).isEqualTo("alpha");
        assertThat(terms.get(1).value()).isEqualTo("beta");
    }

    @Test
    void allTerms_returns_empty_list_for_unknown_uri() {
        assertThat(registry.allTerms("urn:unknown")).isEmpty();
    }

    // --- resolve() tests ---

    @Test
    void typed_resolve_by_primary_value() {
        registry.register(SourceTerm.class);
        assertThat(registry.resolve(SourceTerm.class, "alpha")).contains(SourceTerm.ALPHA);
    }

    @Test
    void typed_resolve_by_alias() {
        registry.register(SourceTerm.class);
        assertThat(registry.resolve(SourceTerm.class, "one")).contains(SourceTerm.ALPHA);
    }

    @Test
    void typed_resolve_missing_value_returns_empty() {
        registry.register(SourceTerm.class);
        assertThat(registry.resolve(SourceTerm.class, "gamma")).isEmpty();
    }

    @Test
    void typed_resolve_unregistered_vocab_returns_empty() {
        // BypassTarget is documented as never registered anywhere in this test class
        assertThat(registry.resolve(BypassTarget.class, "mapped")).isEmpty();
    }

    @Test
    void string_resolve_by_primary_value() {
        registry.register(SourceTerm.class);
        var term = registry.resolve("urn:test:source", "alpha");
        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("alpha");
    }

    @Test
    void string_resolve_by_alias() {
        registry.register(SourceTerm.class);
        var term = registry.resolve("urn:test:source", "a");
        assertThat(term).isPresent().map(VocabularyTerm::value).contains("alpha");
    }

    @Test
    void string_resolve_unknown_uri_returns_empty() {
        assertThat(registry.resolve("urn:unknown", "alpha")).isEmpty();
    }

    // --- axis-unaware equivalentValues() ---

    @Test
    void typed_equivalentValues_axis_unaware_returns_correct_constant() {
        assertThat(registry.equivalentValues(SourceTerm.ALPHA, TargetTerm.class))
            .contains(TargetTerm.PRIMARY);
    }

    @Test
    void typed_equivalentValues_axis_unaware_not_covered_returns_empty() {
        assertThat(registry.equivalentValues(SourceTerm.BETA, TargetTerm.class)).isEmpty();
    }

    @Test
    void string_equivalentValues_axis_unaware_returns_value() {
        registry.register(SourceTerm.class);
        registry.register(TargetTerm.class);
        assertThat(registry.equivalentValues("urn:test:source", "alpha", "urn:test:target"))
            .contains("primary");
    }

    @Test
    void string_equivalentValues_alias_resolves_source() {
        registry.register(SourceTerm.class);
        registry.register(TargetTerm.class);
        assertThat(registry.equivalentValues("urn:test:source", "one", "urn:test:target"))
            .contains("primary");
    }

    @Test
    void string_equivalentValues_unknown_source_uri_returns_empty() {
        assertThat(registry.equivalentValues("urn:unknown", "alpha", "urn:test:target")).isEmpty();
    }

    @Test
    void string_equivalentValues_unknown_target_uri_returns_empty() {
        registry.register(SourceTerm.class);
        assertThat(registry.equivalentValues("urn:test:source", "alpha", "urn:unknown")).isEmpty();
    }

    @Test
    void string_equivalentValues_unknown_value_returns_empty() {
        registry.register(SourceTerm.class);
        registry.register(TargetTerm.class);
        assertThat(registry.equivalentValues("urn:test:source", "gamma", "urn:test:target")).isEmpty();
    }

    // --- axis-aware equivalentValues() ---

    @Test
    void typed_equivalentValues_axis_aware_returns_correct_constant() {
        assertThat(registry.equivalentValues(
                AxisSourceTerm.DTERM, AxisTargetTerm.class, DispositionAxis.RISK_APPETITE))
            .contains(AxisTargetTerm.HIGH_RISK);
    }

    @Test
    void typed_equivalentValues_sparse_axis_returns_empty() {
        assertThat(registry.equivalentValues(
                AxisSourceTerm.DTERM, AxisTargetTerm.class, DispositionAxis.SOCIAL_ORIENTATION))
            .isEmpty();
    }

    @Test
    void string_equivalentValues_axis_aware_returns_value() {
        registry.register(AxisSourceTerm.class);
        registry.register(AxisTargetTerm.class);
        assertThat(registry.equivalentValues(
                "urn:test:axis-source", "d-type", "urn:test:axis-target",
                DispositionAxis.RISK_APPETITE))
            .contains("high-risk");
    }

    @Test
    void axis_unaware_overload_against_axis_only_term_returns_empty() {
        assertThat(registry.equivalentValues(AxisSourceTerm.DTERM, AxisTargetTerm.class)).isEmpty();
    }

    // --- Typed path bypasses registration ---

    @Test
    void typed_equivalentValues_bypasses_registration() {
        assertThat(registry.isRegistered("urn:test:bypass-source")).isFalse();
        assertThat(registry.equivalentValues(BypassSource.TERM, BypassTarget.class))
            .contains(BypassTarget.MAPPED);
    }
}
