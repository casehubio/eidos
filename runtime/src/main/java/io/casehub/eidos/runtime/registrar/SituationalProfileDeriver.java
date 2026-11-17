package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.SituationalProfile;
import io.casehub.eidos.vocab.ArchetypeFamily;
import io.casehub.eidos.vocab.ArchetypeTerm;
import io.casehub.eidos.vocab.SituationalProfileDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class SituationalProfileDeriver {

    private SituationalProfileDeriver() {}

    public static AgentDescriptor deriveSituationalProfiles(AgentDescriptor descriptor) {
        if (descriptor.archetype() == null) return descriptor;

        ArchetypeFamily family = resolveFamily(descriptor.archetype());
        if (family == null) return descriptor;

        List<SituationalProfile> authored = descriptor.disposition() != null
                ? descriptor.disposition().situationalProfiles() : List.of();

        var authoredModes = new HashSet<String>();
        for (var p : authored) {
            authoredModes.add(p.mode());
        }

        List<SituationalProfile> defaults = SituationalProfileDefaults.forFamily(family);
        if (defaults.isEmpty()) return descriptor;

        var merged = new ArrayList<>(authored);
        boolean added = false;
        for (var defaultProfile : defaults) {
            if (!authoredModes.contains(defaultProfile.mode())) {
                merged.add(new SituationalProfile(
                        defaultProfile.mode(),
                        defaultProfile.shifts(),
                        "family-default:" + family.name()));
                added = true;
            }
        }

        if (!added) return descriptor;

        var disp = descriptor.disposition() != null
                ? descriptor.disposition() : io.casehub.eidos.api.AgentDisposition.builder().build();

        var updatedDisp = io.casehub.eidos.api.AgentDisposition.builder()
                .socialOrient(disp.socialOrient())
                .ruleFollowing(disp.ruleFollowing())
                .riskAppetite(disp.riskAppetite())
                .autonomy(disp.autonomy())
                .conflictMode(disp.conflictMode())
                .delegation(disp.delegation())
                .dispositionProfile(disp.dispositionProfile())
                .styleProfile(disp.styleProfile())
                .mbtiType(disp.mbtiType())
                .enneagramType(disp.enneagramType())
                .ampdFacets(disp.ampdFacets())
                .situationalProfiles(List.copyOf(merged))
                .build();

        return descriptor.toBuilder().disposition(updatedDisp).build();
    }

    static ArchetypeFamily resolveFamily(String archetype) {
        try {
            return ArchetypeTerm.valueOf(archetype.toUpperCase(Locale.ROOT)).family();
        } catch (IllegalArgumentException ignored) {}

        try {
            return ArchetypeFamily.valueOf(archetype.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        return null;
    }
}
