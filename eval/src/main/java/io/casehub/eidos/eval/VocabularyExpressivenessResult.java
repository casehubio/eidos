package io.casehub.eidos.eval;

import java.util.List;
import java.util.Map;

public record VocabularyExpressivenessResult(
    String profileName,
    Map<String, Integer> expressivenessScores,
    List<String> weakAxes
) {}
