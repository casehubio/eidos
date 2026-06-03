package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.TaskSemanticEnricher;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.OptionalInt;
import java.util.Set;

@DefaultBean
@ApplicationScoped
public class NoOpTaskSemanticEnricher implements TaskSemanticEnricher {
    @Override public Set<String> dispositionAxes(final String cap, final String domain) { return Set.of(); }
    @Override public boolean semanticallyEquivalent(final String a, final String b) { return false; }
    @Override public OptionalInt significance(final String cap, final String domain) { return OptionalInt.empty(); }
}
