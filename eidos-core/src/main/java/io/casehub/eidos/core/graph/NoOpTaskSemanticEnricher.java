package io.casehub.eidos.core.graph;

import io.casehub.eidos.api.TaskSemanticEnricher;

import java.util.OptionalInt;
import java.util.Set;


public class NoOpTaskSemanticEnricher implements TaskSemanticEnricher {
    @Override public Set<String> dispositionAxes(final String cap, final String domain) { return Set.of(); }
    @Override public boolean semanticallyEquivalent(final String a, final String b) { return false; }
    @Override public OptionalInt significance(final String cap, final String domain) { return OptionalInt.empty(); }
}
