package io.casehub.eidos.runtime.validator;

import io.casehub.eidos.api.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class BriefingCoherenceValidator {

    private static final Set<String> EXTRAVERTED_KEYWORDS = Set.of(
        "outgoing", "gregarious", "thinks out loud",
        "extraverted", "extroverted", "social butterfly");
    private static final Set<String> INTROVERTED_KEYWORDS = Set.of(
        "reserved", "introspective", "works alone",
        "introverted", "solitary", "prefers solitude");

    private final VocabularyRegistry vocabRegistry;

    @Inject
    public BriefingCoherenceValidator(VocabularyRegistry vocabRegistry) {
        this.vocabRegistry = vocabRegistry;
    }

    public CoherenceReport validateStructural(AgentDescriptor descriptor) {
        if (descriptor.briefing() == null || descriptor.briefing().isBlank()) {
            return CoherenceReport.ALIGNED;
        }
        if (descriptor.disposition() == null) {
            return CoherenceReport.ALIGNED;
        }

        var violations = new ArrayList<CoherenceViolation>();
        var briefingLower = descriptor.briefing().toLowerCase();

        scanForVocabularyTermContradictions(descriptor, briefingLower, violations);
        checkOrientationContradiction(descriptor, briefingLower, violations);

        if (violations.isEmpty()) return CoherenceReport.ALIGNED;
        var worst = violations.stream()
            .map(CoherenceViolation::level)
            .max(Comparator.naturalOrder())
            .orElse(CoherenceLevel.ALIGNED);
        return new CoherenceReport(worst, violations);
    }

    public CoherenceReport validate(AgentDescriptor descriptor) {
        return validateStructural(descriptor);
    }

    private void scanForVocabularyTermContradictions(AgentDescriptor descriptor,
                                                     String briefingLower,
                                                     List<CoherenceViolation> violations) {
        var declaredTerms = new HashSet<String>();
        for (var axis : DispositionAxis.values()) {
            for (var dv : descriptor.disposition().get(axis)) {
                declaredTerms.add(dv.term().toLowerCase());
            }
        }
        for (var dv : descriptor.disposition().dispositionProfile()) {
            declaredTerms.add(dv.term().toLowerCase());
        }
        if (declaredTerms.isEmpty()) return;

        var usedVocabUris = new HashSet<String>();
        for (var axis : DispositionAxis.values()) {
            descriptor.vocabUriForAxis(axis).ifPresent(usedVocabUris::add);
        }
        if (descriptor.dispositionVocabulary() != null) {
            usedVocabUris.add(descriptor.dispositionVocabulary());
        }

        for (var uri : usedVocabUris) {
            if (!vocabRegistry.isRegistered(uri)) continue;
            for (var term : vocabRegistry.allTerms(uri)) {
                var termValue = term.value().toLowerCase();
                if (termValue.length() < 3) continue;
                if (declaredTerms.contains(termValue)) continue;

                if (matchesWordBoundary(briefingLower, termValue)) {
                    violations.add(new CoherenceViolation(
                        CoherenceLevel.TENSION,
                        "Briefing mentions '" + term.value()
                            + "' (from " + uri + ") which is not among the "
                            + "declared disposition values",
                        term.value(), null,
                        String.join(", ", declaredTerms), term.value()));
                }

                for (var alias : term.aliases()) {
                    var aliasLower = alias.toLowerCase();
                    if (aliasLower.length() < 3) continue;
                    if (declaredTerms.contains(aliasLower)) continue;
                    if (matchesWordBoundary(briefingLower, aliasLower)) {
                        violations.add(new CoherenceViolation(
                            CoherenceLevel.TENSION,
                            "Briefing mentions '" + alias + "' (alias of "
                                + term.value() + " in " + uri
                                + ") which is not among the declared "
                                + "disposition values",
                            alias, null,
                            String.join(", ", declaredTerms), term.value()));
                    }
                }
            }
        }
    }

    private void checkOrientationContradiction(AgentDescriptor descriptor,
                                                String briefingLower,
                                                List<CoherenceViolation> violations) {
        var profile = descriptor.disposition().dispositionProfile();
        if (profile.isEmpty()) return;
        var jungianUri = "urn:casehub:vocab:jungian";
        if (!jungianUri.equals(descriptor.dispositionVocabulary())) return;

        var dominant = profile.stream()
            .max(Comparator.comparingDouble(DispositionValue::weight))
            .orElse(null);
        if (dominant == null) return;

        var resolved = vocabRegistry.resolve(jungianUri, dominant.term());
        if (resolved.isEmpty()) return;
        var term = resolved.get();
        var termValue = dominant.term().toLowerCase();
        boolean dominantIsIntroverted = termValue.endsWith("i");
        boolean dominantIsExtraverted = termValue.endsWith("e");
        if (!dominantIsIntroverted && !dominantIsExtraverted) return;

        boolean briefingImpliesExtraverted = EXTRAVERTED_KEYWORDS.stream()
            .anyMatch(briefingLower::contains);
        boolean briefingImpliesIntroverted = INTROVERTED_KEYWORDS.stream()
            .anyMatch(briefingLower::contains);

        if (briefingImpliesExtraverted && dominantIsIntroverted) {
            violations.add(new CoherenceViolation(
                CoherenceLevel.TENSION,
                "Briefing implies extraverted orientation but dominant function "
                    + term.label() + " is introverted",
                "extraverted keywords", null,
                "introverted (" + dominant.term() + ")", "extraverted"));
        }
        if (briefingImpliesIntroverted && dominantIsExtraverted) {
            violations.add(new CoherenceViolation(
                CoherenceLevel.TENSION,
                "Briefing implies introverted orientation but dominant function "
                    + term.label() + " is extraverted",
                "introverted keywords", null,
                "extraverted (" + dominant.term() + ")", "introverted"));
        }
    }

    private static boolean matchesWordBoundary(String text, String term) {
        var pattern = Pattern.compile("\\b" + Pattern.quote(term) + "\\b",
            Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }
}
