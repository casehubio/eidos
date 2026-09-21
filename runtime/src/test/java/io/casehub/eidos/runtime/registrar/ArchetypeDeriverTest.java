package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.vocab.ArchetypeFamily;
import io.casehub.eidos.vocab.ArchetypeTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchetypeDeriverTest {

    private static AgentDescriptor.Builder base() {
        return AgentDescriptor.builder()
            .agentId("test").name("Test").slot("analyst").tenancyId("t1");
    }

    @Test
    void explicitArchetypeNotOverridden() {
        var d = base()
            .archetype("mentor")
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isEqualTo("mentor");
    }

    @Test
    void noDispositionReturnsNull() {
        var d = base().build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void mbtiIntjDerivesToSageFamily() {
        var d = base()
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNotNull();
        var term = findTerm(result.archetype());
        assertThat(term).isNotNull();
        assertThat(term.family()).isIn(ArchetypeFamily.SAGE, ArchetypeFamily.MAGICIAN, ArchetypeFamily.SOVEREIGN);
    }

    @Test
    void noVocabularyUriSkipsDerivation() {
        var d = base()
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void emptyProfileSkipsDerivation() {
        var d = base()
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder().build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void domainVocabularyUsedAsFallback() {
        var d = base()
            .domainVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNotNull();
    }

    private static ArchetypeTerm findTerm(String value) {
        for (var t : ArchetypeTerm.values()) {
            if (t.value().equals(value)) return t;
        }
        return null;
    }

// --- Avatar derivation ---

    @Test
    void avatarDerivedWhenArchetypeSetAndAvatarNull() {
        var d      = base().archetype("detective").build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isNotNull();
        assertThat(result.avatar()).startsWith("mythic:P");
    }

    @Test
    void avatarNotOverriddenWhenExplicitlySet() {
        var d = base()
                        .archetype("detective")
                        .avatar("https://example.com/custom.png")
                        .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isEqualTo("https://example.com/custom.png");
    }

    @Test
    void avatarNullWhenNoArchetype() {
        var d      = base().build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isNull();
    }

    @Test
    void avatarCodeMatchesCanonicalIndex() {
        var    d            = base().archetype("detective").build();
        var    result       = ArchetypeDeriver.deriveArchetype(d);
        String expectedCode = io.casehub.eidos.vocab.AvatarCodec.defaultCode("mythic", "detective");
        assertThat(result.avatar()).isEqualTo(expectedCode);
    }
}
