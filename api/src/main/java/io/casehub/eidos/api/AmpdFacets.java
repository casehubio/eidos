package io.casehub.eidos.api;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AmpdFacets {

    public static final Set<String> CANONICAL = Set.of(
            "Anxiousness", "Callousness", "Deceitfulness", "Emotional Lability",
            "Grandiosity", "Hostility", "Impulsivity", "Intimacy Avoidance",
            "Manipulativeness", "Risk Taking", "Suspiciousness", "Withdrawal",
            "Attention Seeking");

    private static final Map<String, String> LOOKUP = CANONICAL.stream()
            .collect(Collectors.toUnmodifiableMap(
                    name -> name.toLowerCase().strip(),
                    name -> name));

    private AmpdFacets() {}

    public static String canonicalise(String facet) {
        if (facet == null || facet.isBlank()) {
            throw new IllegalArgumentException("AMPD facet name must not be blank");
        }
        String canonical = LOOKUP.get(facet.strip().toLowerCase());
        if (canonical == null) {
            throw new IllegalArgumentException("Unknown AMPD facet: " + facet
                    + ". Valid: " + CANONICAL);
        }
        return canonical;
    }
}
