package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    // Cross-vocabulary cycle detection enums — must be class-level for mutual reference
    @VocabularyMetadata(uri = "urn:test:cycle-a")
    enum CrossCycleA implements VocabularyTerm {
        A_TERM("a-term", "A") {
            @Override public List<VocabularyTerm> specializes() {
                return List.of(CrossCycleB.B_TERM);
            }
        };
        final String value, label;
        CrossCycleA(String v, String l) { value = v; label = l; }
        @Override public String value() { return value; }
        @Override public String label() { return label; }
    }

    @VocabularyMetadata(uri = "urn:test:cycle-b")
    enum CrossCycleB implements VocabularyTerm {
        B_TERM("b-term", "B") {
            @Override public List<VocabularyTerm> specializes() {
                return List.of(CrossCycleA.A_TERM);
            }
        };
        final String value, label;
        CrossCycleB(String v, String l) { value = v; label = l; }
        @Override public String value() { return value; }
        @Override public String label() { return label; }
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

    // --- Robustness guards (#42) ---

    @Test
    void register_alias_vs_alias_collision_throws() {
        @VocabularyMetadata(uri = "urn:test:alias-vs-alias", name = "Alias vs Alias", version = "1.0")
        enum AliasVsAlias implements VocabularyTerm {
            TERM_A("a", "A", List.of("shared")),
            TERM_B("b", "B", List.of("shared"));  // "shared" collides with TERM_A's alias
            final String value, label; final List<String> aliases;
            AliasVsAlias(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(AliasVsAlias.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("conflicts");
    }

    @Test
    void register_blank_uri_throws() {
        @VocabularyMetadata(uri = "")
        enum BlankUri implements VocabularyTerm {
            TERM("term", "Term", List.of());
            final String value, label; final List<String> aliases;
            BlankUri(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
            @Override public String value()         { return value; }
            @Override public String label()         { return label; }
            @Override public List<String> aliases() { return aliases; }
        }
        assertThatThrownBy(() -> registry.register(BlankUri.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");
    }

    @Test
    void allTerms_excludes_aliases_when_constants_have_aliases() {
        // SourceTerm.ALPHA aliases ["a","one"], SourceTerm.BETA alias ["b"] — 5 lookup entries, 2 constants
        registry.register(SourceTerm.class);
        var terms = registry.allTerms("urn:test:source");
        assertThat(terms).hasSize(2);
        assertThat(terms).extracting(VocabularyTerm::value)
            .doesNotContain("a", "one", "b");
    }

    // --- vocabularyMetadata() tests ---

    @Test
    void vocabularyMetadata_registered_uri_returns_annotation() {
        registry.register(SourceTerm.class);
        var meta = registry.vocabularyMetadata("urn:test:source");
        assertThat(meta).isPresent();
        assertThat(meta.get().uri()).isEqualTo("urn:test:source");
        assertThat(meta.get().name()).isEqualTo("Source Vocab");
        assertThat(meta.get().version()).isEqualTo("1.0");
    }

    @Test
    void vocabularyMetadata_unregistered_uri_returns_empty() {
        assertThat(registry.vocabularyMetadata("urn:does-not-exist")).isEmpty();
    }

    // --- Hierarchy test vocabulary ---

    @VocabularyMetadata(uri = "urn:test:hierarchy", name = "Hierarchy Vocab", version = "1.0")
    enum HierarchyTerm implements VocabularyTerm {
        ROOT("root", "Root"),
        CHILD_A("child-a", "Child A") {
            @Override public List<VocabularyTerm> specializes() { return List.of(ROOT); }
        },
        CHILD_B("child-b", "Child B") {
            @Override public List<VocabularyTerm> specializes() { return List.of(ROOT); }
        },
        GRANDCHILD("grandchild", "Grandchild") {
            @Override public List<VocabularyTerm> specializes() { return List.of(CHILD_A); }
        },
        DAG_LEAF("dag-leaf", "DAG Leaf") {
            @Override public List<VocabularyTerm> specializes() { return List.of(CHILD_A, CHILD_B); }
        };

        final String value, label;
        HierarchyTerm(String v, String l) { value = v; label = l; }
        @Override public String value() { return value; }
        @Override public String label() { return label; }
    }

    // --- Hierarchy / subsumption tests ---

    @Test
    void subsumes_parent_subsumes_child() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "root", "child-a")).isTrue();
    }

    @Test
    void subsumes_grandparent_subsumes_grandchild() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "root", "grandchild")).isTrue();
    }

    @Test
    void subsumes_self() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "root", "root")).isTrue();
    }

    @Test
    void subsumes_child_does_not_subsume_parent() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "child-a", "root")).isFalse();
    }

    @Test
    void subsumes_siblings_do_not_subsume() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "child-a", "child-b")).isFalse();
    }

    @Test
    void subsumes_unknown_vocab_returns_false() {
        assertThat(registry.subsumes("urn:unknown", "a", "b")).isFalse();
    }

    @Test
    void subsumes_unknown_term_returns_false() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.subsumes("urn:test:hierarchy", "root", "nonexistent")).isFalse();
    }

    @Test
    void match_exact() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "child-a", "child-a"))
            .isEqualTo(new MatchDegree.Exact());
    }

    @Test
    void match_plugin_immediate_parent() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "root", "child-a"))
            .isEqualTo(new MatchDegree.Plugin(1));
    }

    @Test
    void match_plugin_grandparent() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "root", "grandchild"))
            .isEqualTo(new MatchDegree.Plugin(2));
    }

    @Test
    void match_specialization_immediate_child() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "child-a", "root"))
            .isEqualTo(new MatchDegree.Specialization(1));
    }

    @Test
    void match_none_for_siblings() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "child-a", "child-b"))
            .isEqualTo(new MatchDegree.None());
    }

    @Test
    void match_unknown_vocab_returns_none() {
        assertThat(registry.match("urn:unknown", "a", "b"))
            .isEqualTo(new MatchDegree.None());
    }

    @Test
    void ancestors_returns_ordered_by_depth() {
        registry.register(HierarchyTerm.class);
        var ancestors = registry.ancestors("urn:test:hierarchy", "grandchild");
        assertThat(ancestors).extracting(VocabularyTerm::value)
            .containsExactly("child-a", "root");
    }

    @Test
    void ancestors_of_root_is_empty() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.ancestors("urn:test:hierarchy", "root")).isEmpty();
    }

    @Test
    void ancestors_unknown_term_returns_empty() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.ancestors("urn:test:hierarchy", "nonexistent")).isEmpty();
    }

    @Test
    void descendants_returns_ordered_by_depth() {
        registry.register(HierarchyTerm.class);
        var desc = registry.descendants("urn:test:hierarchy", "root");
        assertThat(desc).extracting(VocabularyTerm::value)
            .startsWith("child-a", "child-b");
        assertThat(desc).extracting(VocabularyTerm::value)
            .contains("grandchild", "dag-leaf");
    }

    @Test
    void descendants_of_leaf_is_empty() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.descendants("urn:test:hierarchy", "grandchild")).isEmpty();
    }

    @Test
    void dag_leaf_has_two_parents() {
        registry.register(HierarchyTerm.class);
        var ancestors = registry.ancestors("urn:test:hierarchy", "dag-leaf");
        assertThat(ancestors).extracting(VocabularyTerm::value)
            .contains("child-a", "child-b", "root");
    }

    @Test
    void dag_leaf_min_depth_to_root_is_2() {
        registry.register(HierarchyTerm.class);
        assertThat(registry.match("urn:test:hierarchy", "root", "dag-leaf"))
            .isEqualTo(new MatchDegree.Plugin(2));
    }

    @Test
    void expandForMatchingByVocabulary_returns_scoped_expansion() {
        registry.register(HierarchyTerm.class);
        var expansion = registry.expandForMatchingByVocabulary("child-a");
        assertThat(expansion).containsKey("urn:test:hierarchy");
        var names = expansion.get("urn:test:hierarchy");
        assertThat(names).contains("child-a", "root", "grandchild", "dag-leaf");
    }

    @Test
    void expandForMatchingByVocabulary_unknown_term_returns_empty() {
        var expansion = registry.expandForMatchingByVocabulary("nonexistent");
        assertThat(expansion).isEmpty();
    }

    @Test
    void register_cycle_throws() {
        @VocabularyMetadata(uri = "urn:test:cycle")
        enum CycleTerm implements VocabularyTerm {
            A("a", "A") {
                @Override public List<VocabularyTerm> specializes() { return List.of(B); }
            },
            B("b", "B") {
                @Override public List<VocabularyTerm> specializes() { return List.of(A); }
            };
            final String value, label;
            CycleTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        assertThatThrownBy(() -> registry.register(CycleTerm.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cycle")
            .hasMessageContaining("a (urn:test:cycle)")
            .hasMessageContaining("b (urn:test:cycle)");
    }

    @Test
    void register_cross_vocab_specializes_accepted() {
        @VocabularyMetadata(uri = "urn:test:cross-ref")
        enum CrossRefTerm implements VocabularyTerm {
            X("x", "X") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(SourceTerm.ALPHA);
                }
            };
            final String value, label;
            CrossRefTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(SourceTerm.class);
        registry.register(CrossRefTerm.class);

        assertThat(registry.subsumes("urn:test:cross-ref", "alpha", "x")).isTrue();
        assertThat(registry.ancestors("urn:test:cross-ref", "x"))
            .extracting(VocabularyTerm::value).containsExactly("alpha");
    }

    // --- Cross-vocabulary match() tests ---

    @Test
    void match_cross_vocab_specialization() {
        registry.register(SourceTerm.class);
        @VocabularyMetadata(uri = "urn:test:app-cap")
        enum AppCap implements VocabularyTerm {
            SPECIAL("special", "Special") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(SourceTerm.ALPHA);
                }
            };
            final String value, label;
            AppCap(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(AppCap.class);

        // Specialization: declared is app-specific, requested is foundation
        assertThat(registry.match("urn:test:app-cap", "special", "alpha"))
            .isEqualTo(new MatchDegree.Specialization(1));

        // Plugin: declared is foundation, requested is app-specific (via injection)
        assertThat(registry.match("urn:test:source", "alpha", "special"))
            .isEqualTo(new MatchDegree.Plugin(1));
    }

    // --- Cross-vocabulary subsumes/ancestors/descendants tests ---

    @Test
    void subsumes_cross_vocab_parent_subsumes_child() {
        registry.register(SourceTerm.class);
        @VocabularyMetadata(uri = "urn:test:cross-sub")
        enum CrossSubTerm implements VocabularyTerm {
            LEAF("leaf", "Leaf") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(SourceTerm.ALPHA);
                }
            };
            final String value, label;
            CrossSubTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(CrossSubTerm.class);

        // Within the child's vocab: the cross-vocab parent subsumes the child
        assertThat(registry.subsumes("urn:test:cross-sub", "alpha", "leaf")).isTrue();
        // Reverse: the child does NOT subsume the parent
        assertThat(registry.subsumes("urn:test:cross-sub", "leaf", "alpha")).isFalse();
    }

    @Test
    void ancestors_cross_vocab_returns_cross_vocab_ancestor() {
        registry.register(SourceTerm.class);
        @VocabularyMetadata(uri = "urn:test:cross-anc")
        enum CrossAncTerm implements VocabularyTerm {
            DEEP("deep", "Deep") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(SourceTerm.ALPHA);
                }
            };
            final String value, label;
            CrossAncTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(CrossAncTerm.class);

        // Ancestors of "deep" in cross-anc vocab should include "alpha" from source vocab
        var ancestors = registry.ancestors("urn:test:cross-anc", "deep");
        assertThat(ancestors).extracting(VocabularyTerm::value).containsExactly("alpha");
    }

    @Test
    void descendants_cross_vocab_injection_shows_cross_vocab_child() {
        registry.register(SourceTerm.class);
        @VocabularyMetadata(uri = "urn:test:cross-desc")
        enum CrossDescTerm implements VocabularyTerm {
            BRANCH("branch", "Branch") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(SourceTerm.ALPHA);
                }
            };
            final String value, label;
            CrossDescTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(CrossDescTerm.class);

        // In the source vocab, "alpha" should have "branch" as a descendant (via injection)
        var descendants = registry.descendants("urn:test:source", "alpha");
        assertThat(descendants).extracting(VocabularyTerm::value).contains("branch");
    }

    @Test
    void cross_vocab_late_register_rebuilds_global_dag() {
        // Register parent first, then child — late register() rebuilds global DAG
        // and injects the child into the parent's descendant index
        @VocabularyMetadata(uri = "urn:test:late-parent")
        enum LateParent implements VocabularyTerm {
            BASE("late-base", "Late Base");
            final String value, label;
            LateParent(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        @VocabularyMetadata(uri = "urn:test:late-child")
        enum LateChild implements VocabularyTerm {
            LEAF("late-leaf", "Late Leaf") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(LateParent.BASE);
                }
            };
            final String value, label;
            LateChild(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(LateParent.class);
        // Parent has no descendants yet
        assertThat(registry.descendants("urn:test:late-parent", "late-base")).isEmpty();

        registry.register(LateChild.class);
        // After child registration, global DAG is rebuilt — parent now has the child as descendant
        assertThat(registry.descendants("urn:test:late-parent", "late-base"))
            .extracting(VocabularyTerm::value).contains("late-leaf");
        assertThat(registry.subsumes("urn:test:late-child", "late-base", "late-leaf")).isTrue();
        assertThat(registry.ancestors("urn:test:late-child", "late-leaf"))
            .extracting(VocabularyTerm::value).containsExactly("late-base");
    }

    // --- Cross-vocabulary validation tests ---

    @Test
    void register_cross_vocab_unregistered_parent_vocab_throws() {
        // Use a dedicated never-registered vocab as the parent, avoiding test isolation issues
        // where another test might have registered the parent before this test runs
        @VocabularyMetadata(uri = "urn:test:never-registered-parent")
        enum NeverRegisteredParent implements VocabularyTerm {
            PHANTOM("phantom", "Phantom");
            final String value, label;
            NeverRegisteredParent(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        @VocabularyMetadata(uri = "urn:test:orphan-ref")
        enum OrphanRefTerm implements VocabularyTerm {
            ORPHAN("orphan", "Orphan") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(NeverRegisteredParent.PHANTOM);
                }
            };
            final String value, label;
            OrphanRefTerm(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        // NeverRegisteredParent is NOT registered — this should fail
        assertThatThrownBy(() -> registry.register(OrphanRefTerm.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unregistered vocabulary");
    }

    @Test
    void cross_vocab_cycle_caught_as_unregistered_parent() {
        // Cross-vocabulary cycles are caught at registration time: when CycleA is registered,
        // it references CycleB which isn't registered yet → "unregistered vocabulary" error.
        // This is a stricter check that fires before cycle detection.
        assertThatThrownBy(() -> registry.register(CrossCycleA.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unregistered vocabulary");
    }

    @Test
    void native_vs_injected_value_collision_throws() {
        // Parent vocab has a term "shared"
        @VocabularyMetadata(uri = "urn:test:collision-parent")
        enum ColParent implements VocabularyTerm {
            SHARED("shared", "Shared Parent");
            final String value, label;
            ColParent(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        // Child vocab also has a native term "shared" AND specializes from ColParent.SHARED
        @VocabularyMetadata(uri = "urn:test:collision-child")
        enum ColChild implements VocabularyTerm {
            CHILD_SPEC("child-spec", "Child Spec") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(ColParent.SHARED);
                }
            };
            final String value, label;
            ColChild(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        // ColChild.CHILD_SPEC specializes ColParent.SHARED.
        // When building the parent vocab index, ColChild.CHILD_SPEC would be injected.
        // No collision here because the values are different ("shared" vs "child-spec").
        // Let's create a scenario where there IS a collision:
        @VocabularyMetadata(uri = "urn:test:collision-overlay")
        enum ColOverlay implements VocabularyTerm {
            // This term has the same value "shared" as ColParent.SHARED and specializes from it
            SHARED("shared", "Shared Overlay") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(ColParent.SHARED);
                }
            };
            final String value, label;
            ColOverlay(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(ColParent.class);
        // ColOverlay.SHARED has value "shared" and specializes ColParent.SHARED (also value "shared")
        // When injecting into collision-parent's index, "shared" already exists as native → collision
        assertThatThrownBy(() -> registry.register(ColOverlay.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Value collision");
    }

    @Test
    void late_register_atomicity_on_collision() {
        // Register parent vocab
        @VocabularyMetadata(uri = "urn:test:atom-parent")
        enum AtomParent implements VocabularyTerm {
            BASE("base", "Base");
            final String value, label;
            AtomParent(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(AtomParent.class);

        // Verify parent is registered and has hierarchy
        assertThat(registry.isRegistered("urn:test:atom-parent")).isTrue();

        // Now try to register a vocab that will cause a collision
        @VocabularyMetadata(uri = "urn:test:atom-collision")
        enum AtomCollision implements VocabularyTerm {
            BASE("base", "Base Collision") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(AtomParent.BASE);
                }
            };
            final String value, label;
            AtomCollision(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }

        // This should fail with collision — "base" already exists in atom-parent
        assertThatThrownBy(() -> registry.register(AtomCollision.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Value collision");

        // After the failed registration, the colliding vocab should NOT be registered
        assertThat(registry.isRegistered("urn:test:atom-collision")).isFalse();

        // And the existing vocab's indexes should be intact
        assertThat(registry.isRegistered("urn:test:atom-parent")).isTrue();
        assertThat(registry.allTerms("urn:test:atom-parent")).hasSize(1);
    }

    @Test
    void flat_vocab_has_no_hierarchy() {
        // Use a dedicated vocab that no other test creates cross-vocab specializations of,
        // avoiding test isolation issues with the shared @ApplicationScoped registry
        @VocabularyMetadata(uri = "urn:test:flat-isolated")
        enum FlatIsolated implements VocabularyTerm {
            P("p", "P"),
            Q("q", "Q");
            final String value, label;
            FlatIsolated(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(FlatIsolated.class);
        assertThat(registry.ancestors("urn:test:flat-isolated", "p")).isEmpty();
        assertThat(registry.descendants("urn:test:flat-isolated", "p")).isEmpty();
        assertThat(registry.subsumes("urn:test:flat-isolated", "p", "q")).isFalse();
        assertThat(registry.match("urn:test:flat-isolated", "p", "q"))
            .isEqualTo(new MatchDegree.None());
    }

    @Test
    void injected_vs_injected_value_collision_throws() {
        // Foundation vocab with two parent terms
        @VocabularyMetadata(uri = "urn:test:foundation")
        enum Foundation implements VocabularyTerm {
            DOCUMENTATION("documentation", "Documentation"),
            ANALYSIS("analysis", "Analysis");
            final String value, label;
            Foundation(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        // Clinical defines "review" specializing Foundation.DOCUMENTATION
        @VocabularyMetadata(uri = "urn:test:clinical")
        enum Clinical implements VocabularyTerm {
            REVIEW("review", "Clinical Review") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(Foundation.DOCUMENTATION);
                }
            };
            final String value, label;
            Clinical(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        // Legal defines "review" specializing Foundation.ANALYSIS
        @VocabularyMetadata(uri = "urn:test:legal")
        enum Legal implements VocabularyTerm {
            REVIEW("review", "Legal Review") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(Foundation.ANALYSIS);
                }
            };
            final String value, label;
            Legal(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(Foundation.class);
        registry.register(Clinical.class);
        // Legal.REVIEW will attempt to inject into Foundation's index under "review",
        // but Clinical.REVIEW already injected there → collision
        assertThatThrownBy(() -> registry.register(Legal.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("collision")
            .hasMessageContaining("urn:test:foundation")
            .hasMessageContaining("urn:test:clinical")
            .hasMessageContaining("urn:test:legal");
    }

    // --- Cross-vocabulary expandForMatchingByVocabulary tests ---

    @Test
    void expandForMatchingByVocabulary_cross_vocab_groups_by_declaring_vocab() {
        @VocabularyMetadata(uri = "urn:test:expand-base")
        enum ExpandBase implements VocabularyTerm {
            ROOT("expand-root", "Expand Root");
            final String value, label;
            ExpandBase(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        @VocabularyMetadata(uri = "urn:test:expand-app")
        enum ExpandApp implements VocabularyTerm {
            APP_CHILD("expand-app-child", "Expand App Child") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(ExpandBase.ROOT);
                }
            };
            final String value, label;
            ExpandApp(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(ExpandBase.class);
        registry.register(ExpandApp.class);

        // Expanding foundation term — should include app-tier descendant
        var expansion = registry.expandForMatchingByVocabulary("expand-root");
        assertThat(expansion).containsKey("urn:test:expand-base");
        assertThat(expansion).containsKey("urn:test:expand-app");
        assertThat(expansion.get("urn:test:expand-app")).contains("expand-app-child");

        // Expanding app term — should include foundation ancestor
        var appExpansion = registry.expandForMatchingByVocabulary("expand-app-child");
        assertThat(appExpansion).containsKey("urn:test:expand-app");
        assertThat(appExpansion).containsKey("urn:test:expand-base");
        assertThat(appExpansion.get("urn:test:expand-base")).contains("expand-root");
    }

    @Test
    void expandForMatchingByVocabulary_three_vocab_chain_expansion() {
        @VocabularyMetadata(uri = "urn:test:chain-foundation")
        enum ChainFoundation implements VocabularyTerm {
            BASE("chain-base", "Chain Base");
            final String value, label;
            ChainFoundation(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        @VocabularyMetadata(uri = "urn:test:chain-mid")
        enum ChainMid implements VocabularyTerm {
            MID("chain-mid", "Chain Mid") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(ChainFoundation.BASE);
                }
            };
            final String value, label;
            ChainMid(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        @VocabularyMetadata(uri = "urn:test:chain-app")
        enum ChainApp implements VocabularyTerm {
            APP("chain-app", "Chain App") {
                @Override public List<VocabularyTerm> specializes() {
                    return List.of(ChainMid.MID);
                }
            };
            final String value, label;
            ChainApp(String v, String l) { value = v; label = l; }
            @Override public String value() { return value; }
            @Override public String label() { return label; }
        }
        registry.register(ChainFoundation.class);
        registry.register(ChainMid.class);
        registry.register(ChainApp.class);

        // Expanding foundation term — should include mid and app descendants grouped by their vocabularies
        var baseExpansion = registry.expandForMatchingByVocabulary("chain-base");
        assertThat(baseExpansion).containsKey("urn:test:chain-foundation");
        assertThat(baseExpansion).containsKey("urn:test:chain-mid");
        assertThat(baseExpansion).containsKey("urn:test:chain-app");
        assertThat(baseExpansion.get("urn:test:chain-foundation")).contains("chain-base");
        assertThat(baseExpansion.get("urn:test:chain-mid")).contains("chain-mid");
        assertThat(baseExpansion.get("urn:test:chain-app")).contains("chain-app");

        // Expanding mid term — should include foundation ancestor and app descendant
        var midExpansion = registry.expandForMatchingByVocabulary("chain-mid");
        assertThat(midExpansion).containsKey("urn:test:chain-foundation");
        assertThat(midExpansion).containsKey("urn:test:chain-mid");
        assertThat(midExpansion).containsKey("urn:test:chain-app");
        assertThat(midExpansion.get("urn:test:chain-foundation")).contains("chain-base");
        assertThat(midExpansion.get("urn:test:chain-mid")).contains("chain-mid");
        assertThat(midExpansion.get("urn:test:chain-app")).contains("chain-app");

        // Expanding app term — should include both foundation and mid ancestors
        var appExpansion = registry.expandForMatchingByVocabulary("chain-app");
        assertThat(appExpansion).containsKey("urn:test:chain-foundation");
        assertThat(appExpansion).containsKey("urn:test:chain-mid");
        assertThat(appExpansion).containsKey("urn:test:chain-app");
        assertThat(appExpansion.get("urn:test:chain-foundation")).contains("chain-base");
        assertThat(appExpansion.get("urn:test:chain-mid")).contains("chain-mid");
        assertThat(appExpansion.get("urn:test:chain-app")).contains("chain-app");
    }
}
