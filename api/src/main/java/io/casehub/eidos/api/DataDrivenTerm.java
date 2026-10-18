package io.casehub.eidos.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record DataDrivenTerm(
    String value, String label, String description,
    List<String> aliases, List<String> keywords,
    List<VocabularyTerm> resolvedParents
) implements VocabularyTerm {

    public DataDrivenTerm {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("DataDrivenTerm.value must not be blank");
        if (label == null) label = "";
        if (description == null) description = "";
        if (aliases == null) aliases = List.of();
        if (keywords == null) keywords = List.of();
        if (resolvedParents == null) resolvedParents = List.of();

        AgentDescriptorValidator.validateChars("value", value);
        if (!label.isEmpty()) AgentDescriptorValidator.validateChars("label", label);
        if (!description.isEmpty()) AgentDescriptorValidator.validateChars("description", description);
        for (var a : aliases) AgentDescriptorValidator.validateChars("alias", a);
        for (var k : keywords) {
            if (k == null || k.isBlank())
                throw new IllegalArgumentException("DataDrivenTerm keywords must not be blank");
            AgentDescriptorValidator.validateChars("keyword", k);
        }

        var deduped = new ArrayList<>(new LinkedHashSet<>(keywords));
        keywords = List.copyOf(deduped);
        aliases = List.copyOf(aliases);
        resolvedParents = List.copyOf(resolvedParents);
    }

    @Override public List<VocabularyTerm> specializes() { return resolvedParents; }
}
