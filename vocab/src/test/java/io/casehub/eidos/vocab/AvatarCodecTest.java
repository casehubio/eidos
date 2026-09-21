package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarCodecTest {

    @Test
    void detectiveIndexIsStable() {
        int idx = AvatarCodec.archetypeIndex("detective");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        assertThat(idx).isLessThan(48);
    }

    @Test
    void indexIsSortedByFamilyThenValue() {
        int angel = AvatarCodec.archetypeIndex("angel");
        int guardian = AvatarCodec.archetypeIndex("guardian");
        int artist = AvatarCodec.archetypeIndex("artist");
        assertThat(angel).isLessThan(guardian);
        assertThat(guardian).isLessThan(artist);
    }

    @Test
    void defaultCodeFormatsAsPreset() {
        String code = AvatarCodec.defaultCode("mythic", "detective");
        assertThat(code).startsWith("mythic:P");
    }

    @Test
    void defaultCodeForUnknownArchetypeReturnsNull() {
        assertThat(AvatarCodec.defaultCode("mythic", "nonexistent")).isNull();
    }

    @Test
    void isExternalUrlDetectsHttps() {
        assertThat(AvatarCodec.isExternalUrl("https://example.com/img.png")).isTrue();
        assertThat(AvatarCodec.isExternalUrl("http://example.com/img.png")).isTrue();
    }

    @Test
    void isExternalUrlRejectsCodes() {
        assertThat(AvatarCodec.isExternalUrl("mythic:P1B")).isFalse();
        assertThat(AvatarCodec.isExternalUrl(null)).isFalse();
    }

    @Test
    void allArchetypesHaveUniqueIndices() {
        var indices = new HashSet<Integer>();
        for (var term : ArchetypeTerm.values()) {
            int idx = AvatarCodec.archetypeIndex(term.value());
            assertThat(indices.add(idx))
                .as("duplicate index for " + term.value())
                .isTrue();
        }
        assertThat(indices).hasSize(48);
    }

    @Test
    void defaultCollectionIsMythic() {
        assertThat(AvatarCodec.DEFAULT_COLLECTION).isEqualTo("mythic");
    }

    @Test
    void caregiverAngelIsIndex0() {
        assertThat(AvatarCodec.archetypeIndex("angel")).isEqualTo(0);
        assertThat(AvatarCodec.defaultCode("mythic", "angel")).isEqualTo("mythic:P0");
    }

    @Test
    void sovereignRulerIsIndex46() {
        assertThat(AvatarCodec.archetypeIndex("ruler")).isEqualTo(47);
    }
}
