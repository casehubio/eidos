package io.casehub.eidos.eval;

import io.casehub.eidos.api.DispositionAxis;

import java.util.ArrayList;
import java.util.List;

import static io.casehub.eidos.api.DispositionAxis.*;

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
    private static final List<DispositionAxis> AXES = List.of(
        SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY
    );

    public static PersonalityPreservationReport build(
        final List<VocabularyExpressivenessResult> exp,
        final List<TraitExpressionResult> traits,
        final List<PairContrastResult> contrasts
    ) {
        final double meanExp = exp.stream()
            .flatMapToInt(r -> r.expressivenessScores().values().stream().mapToInt(Integer::intValue))
            .average().orElse(0.0);

        final long totalMatches = traits.stream()
            .flatMap(r -> r.directionMatches().values().stream())
            .filter(Boolean::booleanValue).count();
        final long totalCells = traits.stream()
            .mapToLong(r -> r.directionMatches().size()).sum();
        final double meanTraitMatchRate = totalCells > 0 ? (double) totalMatches / totalCells : 0.0;

        final double meanEffectSize = contrasts.stream()
            .mapToInt(PairContrastResult::effectSize).average().orElse(0.0);

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
            for (final DispositionAxis axis : AXES) {
                // expressivenessScores is Map<String, Integer> — bridge via jsonKey()
                final int s1 = er.expressivenessScores().getOrDefault(axis.jsonKey(), -1);

                final List<TraitExpressionResult> profileTraits = traits.stream()
                    .filter(t -> t.evalCase().profile().name().equals(er.profileName()))
                    .toList();

                // directionMatches is Map<String, Boolean> — bridge via jsonKey()
                final double matchRate = profileTraits.isEmpty() ? -1.0 :
                    profileTraits.stream()
                        .mapToInt(t -> Boolean.TRUE.equals(t.directionMatches().get(axis.jsonKey())) ? 1 : 0)
                        .average().orElse(-1.0);

                // expressionScores is Map<String, Integer> — bridge via jsonKey()
                final double s2Score = profileTraits.isEmpty() ? -1.0 :
                    profileTraits.stream()
                        .mapToInt(t -> t.expressionScores().getOrDefault(axis.jsonKey(), -1))
                        .filter(s -> s >= 0)
                        .average().orElse(-1.0);

                // primaryAxis is DispositionAxis — enum equality
                final double s3 = contrasts.stream()
                    .filter(c -> c.primaryAxis() == axis
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

                // AttributionDiagnosis.axis is String — bridge via jsonKey()
                result.add(new AttributionDiagnosis(
                    er.profileName(), axis.jsonKey(), s1,
                    s2Score < 0 ? -1 : (int) Math.round(s2Score),
                    (int) Math.round(s3 < 0 ? -1 : s3),
                    attr));
            }
        }
        return result;
    }
}
