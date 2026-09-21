package io.casehub.eidos.vocab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArchetypeResolver {

    public sealed interface Resolution permits Converged, Narrowed, Conflict {}
    public record Converged(ArchetypeTerm archetype) implements Resolution {}
    public record Narrowed(List<ArchetypeTerm> candidates) implements Resolution {}
    public record Conflict(String frameworkUriA, String termValueA,
                           String frameworkUriB, String termValueB) implements Resolution {}

    private ArchetypeResolver() {}

    public static Resolution resolve(Map<String, String> frameworkValues) {
        if (frameworkValues.isEmpty()) {
            return new Narrowed(List.of(ArchetypeTerm.values()));
        }

        Set<ArchetypeFamily> candidateFamilies = EnumSet.allOf(ArchetypeFamily.class);

        for (var entry : frameworkValues.entrySet()) {
            var families = ArchetypeCompatibility.compatibleFamilies(
                entry.getKey(), entry.getValue());
            candidateFamilies.retainAll(families);

            if (candidateFamilies.isEmpty()) {
                return findConflictingPair(frameworkValues);
            }
        }

        List<ArchetypeTerm> candidates = Arrays.stream(ArchetypeTerm.values())
            .filter(t -> candidateFamilies.contains(t.family()))
            .toList();

        if (candidates.isEmpty()) {
            return findConflictingPair(frameworkValues);
        }
        if (candidates.size() == 1) {
            return new Converged(candidates.get(0));
        }

        List<ArchetypeTerm> affinityMatched = candidates.stream()
            .filter(t -> frameworkValues.entrySet().stream()
                .anyMatch(e -> ArchetypeCompatibility
                    .subArchetypeAffinity(t, e.getKey(), e.getValue())))
            .toList();

        if (affinityMatched.size() == 1) {
            return new Converged(affinityMatched.get(0));
        }
        if (!affinityMatched.isEmpty()) {
            return new Narrowed(affinityMatched);
        }

        return new Narrowed(candidates);
    }

    private static Conflict findConflictingPair(Map<String, String> frameworkValues) {
        var entries = new ArrayList<>(frameworkValues.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                var a = ArchetypeCompatibility.compatibleFamilies(
                    entries.get(i).getKey(), entries.get(i).getValue());
                var b = ArchetypeCompatibility.compatibleFamilies(
                    entries.get(j).getKey(), entries.get(j).getValue());
                var intersection = new HashSet<>(a);
                intersection.retainAll(b);
                if (intersection.isEmpty()) {
                    return new Conflict(
                        entries.get(i).getKey(), entries.get(i).getValue(),
                        entries.get(j).getKey(), entries.get(j).getValue());
                }
            }
        }
        return new Conflict(entries.get(0).getKey(), entries.get(0).getValue(),
                           entries.get(1).getKey(), entries.get(1).getValue());
    }
}
