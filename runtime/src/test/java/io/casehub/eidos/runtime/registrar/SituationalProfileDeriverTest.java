package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AmpdFacetShift;
import io.casehub.eidos.api.SituationalProfile;
import io.casehub.eidos.vocab.ArchetypeFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SituationalProfileDeriverTest {

    @Test
    void derivesProfilesFromArchetypeTerm() {
        var descriptor = minimalDescriptor("warrior");
        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        assertThat(derived.disposition().situationalProfiles()).hasSize(5);
        assertThat(derived.disposition().situationalProfiles())
                .allSatisfy(p -> assertThat(p.provenance()).startsWith("family-default:HERO"));
    }

    @Test
    void derivesProfilesFromArchetypeFamily() {
        var descriptor = minimalDescriptor("CAREGIVER");
        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        assertThat(derived.disposition().situationalProfiles()).hasSize(5);
    }

    @Test
    void noArchetypeLeavesDescriptorUnchanged() {
        var descriptor = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .build();
        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        assertThat(derived).isSameAs(descriptor);
    }

    @Test
    void unknownArchetypeLeavesDescriptorUnchanged() {
        var descriptor = minimalDescriptor("completely_unknown");
        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        assertThat(derived).isSameAs(descriptor);
    }

    @Test
    void authoredModesArePreserved() {
        var authored = new SituationalProfile("stress",
                List.of(new AmpdFacetShift("Callousness", 30)), "authored");
        var disp = AgentDisposition.builder().situationalProfiles(authored).build();
        var descriptor = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .archetype("warrior").disposition(disp)
                .build();

        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        var profiles = derived.disposition().situationalProfiles();

        assertThat(profiles).hasSize(5);
        var stress = profiles.stream().filter(p -> "stress".equals(p.mode())).findFirst().orElseThrow();
        assertThat(stress.provenance()).isEqualTo("authored");
        assertThat(stress.shifts()).hasSize(1);
        assertThat(stress.shifts().getFirst().facet()).isEqualTo("Callousness");

        var flow = profiles.stream().filter(p -> "flow".equals(p.mode())).findFirst().orElseThrow();
        assertThat(flow.provenance()).startsWith("family-default:");
    }

    @Test
    void allModesAuthoredSkipsDerivation() {
        var profiles = SituationalProfile.VALID_MODES.stream()
                .map(mode -> new SituationalProfile(mode, List.of(), "authored"))
                .toList();
        var disp = AgentDisposition.builder().situationalProfiles(profiles).build();
        var descriptor = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .archetype("warrior").disposition(disp)
                .build();

        var derived = SituationalProfileDeriver.deriveSituationalProfiles(descriptor);
        assertThat(derived).isSameAs(descriptor);
    }

    @Test
    void resolveFamilyFromTerm() {
        assertThat(SituationalProfileDeriver.resolveFamily("warrior")).isEqualTo(ArchetypeFamily.HERO);
        assertThat(SituationalProfileDeriver.resolveFamily("WARRIOR")).isEqualTo(ArchetypeFamily.HERO);
    }

    @Test
    void resolveFamilyFromFamilyName() {
        assertThat(SituationalProfileDeriver.resolveFamily("HERO")).isEqualTo(ArchetypeFamily.HERO);
        assertThat(SituationalProfileDeriver.resolveFamily("caregiver")).isEqualTo(ArchetypeFamily.CAREGIVER);
    }

    @Test
    void resolveFamilyReturnsNullForUnknown() {
        assertThat(SituationalProfileDeriver.resolveFamily("xyz")).isNull();
    }

    private static AgentDescriptor minimalDescriptor(String archetype) {
        return AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .archetype(archetype)
                .build();
    }
}
