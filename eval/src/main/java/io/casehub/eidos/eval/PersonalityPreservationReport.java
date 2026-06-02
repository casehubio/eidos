package io.casehub.eidos.eval;

import java.util.ArrayList;
import java.util.List;

public record PersonalityPreservationReport(
    List<VocabularyExpressivenessResult> expressivenessResults,
    List<TraitExpressionResult> traitExpressionResults,
    List<PairContrastResult> pairContrastResults,
    List<AttributionDiagnosis> diagnoses,
    double meanExpressivenessScore,
    double meanTraitMatchRate,
    double meanEffectSize,
    double discriminationAccuracy,
    List<String> annotations
) {
    private static final List<String> AXES =
        List.of("socialOrient", "ruleFollowing", "riskAppetite", "autonomy");

    public static PersonalityPreservationReport build(
        final List<VocabularyExpressivenessResult> exp,
        final List<TraitExpressionResult> traits,
        final List<PairContrastResult> contrasts
    ) {
        // meanExpressivenessScore: flat mean across all (profile × axis) cells
        final double meanExp = exp.stream()
            .flatMapToInt(r -> r.expressivenessScores().values().stream().mapToInt(Integer::intValue))
            .average().orElse(0.0);

        // meanTraitMatchRate: flat mean across all (profile × format × axis) direction-match cells
        final long totalMatches = traits.stream()
            .flatMap(r -> r.directionMatches().values().stream())
            .filter(Boolean::booleanValue).count();
        final long totalCells = traits.stream()
            .mapToLong(r -> r.directionMatches().size()).sum();
        final double meanTraitMatchRate = totalCells > 0 ? (double) totalMatches / totalCells : 0.0;

        // meanEffectSize: flat mean across all (pair × format) results
        final double meanEffectSize = contrasts.stream()
            .mapToInt(PairContrastResult::effectSize).average().orElse(0.0);

        // discriminationAccuracy: % pairs correctly identified
        final double discAcc = contrasts.isEmpty() ? 0.0 :
            (double) contrasts.stream().filter(PairContrastResult::correctlyIdentified).count()
            / contrasts.size();

        final List<AttributionDiagnosis> diagnoses = computeDiagnoses(exp, traits, contrasts);

        return new PersonalityPreservationReport(
            exp, traits, contrasts, diagnoses,
            meanExp, meanTraitMatchRate, meanEffectSize, discAcc,
            new ArrayList<>()
        );
    }

    private static List<AttributionDiagnosis> computeDiagnoses(
        final List<VocabularyExpressivenessResult> exp,
        final List<TraitExpressionResult> traits,
        final List<PairContrastResult> contrasts
    ) {
        final List<AttributionDiagnosis> result = new ArrayList<>();
        for (final VocabularyExpressivenessResult er : exp) {
            for (final String axis : AXES) {
                final int s1 = er.expressivenessScores().getOrDefault(axis, -1);

                // Stage 2: mean direction match across all formats for this profile × axis
                final List<TraitExpressionResult> profileTraits = traits.stream()
                    .filter(t -> t.evalCase().profile().name().equals(er.profileName()))
                    .toList();
                final double matchRate = profileTraits.isEmpty() ? -1.0 :
                    profileTraits.stream()
                        .mapToInt(t -> Boolean.TRUE.equals(t.directionMatches().get(axis)) ? 1 : 0)
                        .average().orElse(-1.0);

                // Stage 2 actual expression score (mean across formats for this profile × axis)
                final double s2Score = profileTraits.isEmpty() ? -1.0 :
                    profileTraits.stream()
                        .mapToInt(t -> t.expressionScores().getOrDefault(axis, -1))
                        .filter(s -> s >= 0)
                        .average().orElse(-1.0);

                // Stage 3: mean effectSize for this profile × axis from variant pairs
                final double s3 = contrasts.stream()
                    .filter(c -> c.primaryAxis().equals(axis)
                        && (c.profileHigh().equals(er.profileName())
                            || c.profileLow().equals(er.profileName())))
                    .mapToInt(PairContrastResult::effectSize)
                    .average().orElse(-1.0);

                final Attribution attr;
                if (s1 < 0) {
                    attr = Attribution.INSUFFICIENT_DATA;
                } else if (s1 <= 2) {
                    attr = Attribution.VOCABULARY_GAP;
                } else if (s1 >= 4 && matchRate >= 0 && matchRate < 0.5) {
                    attr = Attribution.RENDERER_FLATTENING;
                } else if (s1 >= 4 && matchRate >= 0.5 && s3 > 0 && s3 <= 2) {
                    attr = Attribution.PROFILE_DESIGN_GAP;
                } else if (s1 >= 4 && matchRate >= 0.5 && s3 > 2) {
                    attr = Attribution.WORKING;
                } else {
                    attr = Attribution.INSUFFICIENT_DATA;
                }

                result.add(new AttributionDiagnosis(
                    er.profileName(), axis, s1,
                    s2Score < 0 ? -1 : (int) Math.round(s2Score),
                    (int) Math.round(s3 < 0 ? -1 : s3),
                    attr));
            }
        }
        return result;
    }
}
