package io.casehub.eidos.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class DomainKeywords {
    private DomainKeywords() {}

    public static List<String> expanded(VocabularyRegistry registry,
                                         String vocabUri, String value) {
        var term = registry.resolve(vocabUri, value);
        if (term.isEmpty()) return List.of();
        var result = new LinkedHashSet<>(term.get().keywords());
        for (var ancestor : registry.ancestors(vocabUri, value)) {
            result.addAll(ancestor.keywords());
        }
        return List.copyOf(new ArrayList<>(result));
    }
}
