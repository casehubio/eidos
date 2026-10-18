package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.core.vocabulary.ClasspathYamlVocabularyRegistrar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathYamlVocabularyRegistrarTest {

    @Test void loads_vocabulary_from_yaml() {
        var registrar = new ClasspathYamlVocabularyRegistrar();
        var vocabs = registrar.vocabularies();
        var testVocab = vocabs.stream()
            .filter(v -> v.uri().equals("urn:test:yaml-vocab"))
            .findFirst();
        assertThat(testVocab).isPresent();
        assertThat(testVocab.get().terms()).hasSize(2);
        assertThat(testVocab.get().name()).isEqualTo("Test YAML Vocabulary");
    }

    @Test void resolves_specializes_references() {
        var registrar = new ClasspathYamlVocabularyRegistrar();
        var vocab = registrar.vocabularies().stream()
            .filter(v -> v.uri().equals("urn:test:yaml-vocab")).findFirst().orElseThrow();
        var child = vocab.terms().stream()
            .filter(t -> t.value().equals("child-term")).findFirst().orElseThrow();
        assertThat(child.specializes()).hasSize(1);
        assertThat(child.specializes().get(0).value()).isEqualTo("parent-term");
    }

    @Test void loads_keywords_and_aliases() {
        var registrar = new ClasspathYamlVocabularyRegistrar();
        var vocab = registrar.vocabularies().stream()
            .filter(v -> v.uri().equals("urn:test:yaml-vocab")).findFirst().orElseThrow();
        var parent = vocab.terms().stream()
            .filter(t -> t.value().equals("parent-term")).findFirst().orElseThrow();
        assertThat(parent.keywords()).containsExactly("keyword-a", "keyword-b");
        var child = vocab.terms().stream()
            .filter(t -> t.value().equals("child-term")).findFirst().orElseThrow();
        assertThat(child.aliases()).containsExactly("child-alias");
    }

    @Test void loadFrom_with_bad_specializes_throws() {
        var yaml = """
            uri: "urn:bad"
            name: Bad
            version: "1"
            description: ""
            terms:
              - value: orphan
                label: Orphan
                specializes: [nonexistent]
            """;
        var registrar = new ClasspathYamlVocabularyRegistrar();
        assertThatThrownBy(() -> registrar.loadFrom(
            new java.io.ByteArrayInputStream(yaml.getBytes())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nonexistent");
    }

    @Test
    void three_level_hierarchy_resolves_parents_correctly() {
        var yaml = """
                   uri: "urn:three-level"
                   name: Three Level
                   version: "1"
                   description: ""
                   terms:
                     - value: root
                       label: Root
                     - value: mid
                       label: Mid
                       specializes: [root]
                     - value: leaf
                       label: Leaf
                       specializes: [mid]
                   """;
        var registrar = new ClasspathYamlVocabularyRegistrar();
        var vocab     = registrar.loadFrom(new java.io.ByteArrayInputStream(yaml.getBytes()));
        var leaf      = vocab.terms().stream().filter(t -> t.value().equals("leaf")).findFirst().orElseThrow();
        assertThat(leaf.specializes()).hasSize(1);
        assertThat(leaf.specializes().get(0).value()).isEqualTo("mid");
        var mid = leaf.specializes().get(0);
        assertThat(mid.specializes()).hasSize(1);
        assertThat(mid.specializes().get(0).value()).isEqualTo("root");
    }
}
