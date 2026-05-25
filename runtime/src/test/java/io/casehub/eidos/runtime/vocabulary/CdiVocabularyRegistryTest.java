package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class CdiVocabularyRegistryTest {

    @Inject
    VocabularyRegistry registry;

    static Vocabulary testVocab(String uri) {
        return new Vocabulary(uri, "Test Vocab", "1.0", Map.of(
            "alpha", new VocabularyTerm("alpha", "Alpha", "First", List.of("a", "one"),
                Map.of("urn:other", "primary")),
            "beta",  new VocabularyTerm("beta",  "Beta",  "Second", List.of("b"),
                Map.of("urn:other", "secondary"))
        ));
    }

    @Test
    void programmatic_register_and_find() {
        registry.register(testVocab("urn:test:prog"));

        var found = registry.find("urn:test:prog");

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Test Vocab");
    }

    @Test
    void find_returns_empty_for_unknown_uri() {
        assertThat(registry.find("urn:does-not-exist")).isEmpty();
    }

    @Test
    void resolve_by_exact_value() {
        registry.register(testVocab("urn:test:resolve"));

        var term = registry.resolve("urn:test:resolve", "alpha");

        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("alpha");
    }

    @Test
    void resolve_by_alias() {
        registry.register(testVocab("urn:test:alias"));

        var term = registry.resolve("urn:test:alias", "one");

        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("alpha");
    }

    @Test
    void resolve_returns_empty_for_unknown_value() {
        registry.register(testVocab("urn:test:miss"));

        assertThat(registry.resolve("urn:test:miss", "gamma")).isEmpty();
    }

    @Test
    void equivalent_values_returns_cross_vocab_match() {
        registry.register(testVocab("urn:test:equiv"));

        var equiv = registry.equivalentValues("urn:test:equiv", "alpha", "urn:other");

        assertThat(equiv).containsExactly("primary");
    }

    @Test
    void equivalent_values_via_alias_match() {
        registry.register(testVocab("urn:test:alias-equiv"));

        var equiv = registry.equivalentValues("urn:test:alias-equiv", "one", "urn:other");

        assertThat(equiv).containsExactly("primary");
    }

    @Test
    void equivalent_values_empty_for_no_match() {
        registry.register(testVocab("urn:test:no-match"));

        assertThat(registry.equivalentValues("urn:test:no-match", "alpha", "urn:nonexistent"))
            .isEmpty();
    }

    @Test
    void equivalent_values_returns_empty_for_unknown_source_vocab() {
        assertThat(registry.equivalentValues("urn:unknown:vocab", "any-value", "urn:other"))
            .isEmpty();
    }

    @Test
    void equivalent_values_collects_across_all_matching_terms() {
        // Two distinct terms share the alias "shared". Both map to different values in the
        // target vocabulary. equivalentValues must return both — not just the first match.
        var vocab = new Vocabulary("urn:test:multi-term", "Multi", "1.0", Map.of(
            "alpha", new VocabularyTerm("alpha", "Alpha", "First",
                List.of("shared"), Map.of("urn:target", "value-a")),
            "beta",  new VocabularyTerm("beta",  "Beta",  "Second",
                List.of("shared"), Map.of("urn:target", "value-b"))
        ));
        registry.register(vocab);

        var result = registry.equivalentValues("urn:test:multi-term", "shared", "urn:target");

        assertThat(result).containsExactlyInAnyOrder("value-a", "value-b");
    }

    @Test
    void programmatic_register_overrides_existing() {
        registry.register(testVocab("urn:test:override"));
        registry.register(new Vocabulary("urn:test:override", "Updated", "2.0", Map.of()));

        assertThat(registry.find("urn:test:override").get().name()).isEqualTo("Updated");
    }
}
