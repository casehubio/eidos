package io.casehub.eidos.api;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record DataDrivenVocabulary(
    String uri, String name, String version, String description,
    List<DataDrivenTerm> terms
) {
    public DataDrivenVocabulary {
        if (uri == null || uri.isBlank())
            throw new IllegalArgumentException("DataDrivenVocabulary.uri must not be blank");
        Objects.requireNonNull(terms, "terms must not be null");
        if (terms.isEmpty())
            throw new IllegalArgumentException("DataDrivenVocabulary must have at least one term");
        if (name == null) name = "";
        if (version == null) version = "";
        if (description == null) description = "";
        var seen = new HashSet<String>();
        for (var t : terms) {
            if (!seen.add(t.value()))
                throw new IllegalArgumentException("Duplicate term value: " + t.value());
        }
        terms = List.copyOf(terms);
    }
}
