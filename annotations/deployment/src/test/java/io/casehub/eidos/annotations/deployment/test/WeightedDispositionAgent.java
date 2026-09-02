package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.*;
import io.casehub.eidos.api.DispositionAxis;

@Identity(slot = "weighted-agent", briefing = "Agent with weighted profiles",
          weightsFingerprint = "sha256:abc123", modelVersion = "2024-Q3")
@Disposition(
    socialOrient = "collaborative",
    dispositionProfile = {
        @DispositionWeight(value = "collaborative", weight = 0.8),
        @DispositionWeight(value = "analytical", weight = 0.4)
    },
    styleProfile = {
        @DispositionWeight(value = "concise", weight = 0.7)
    },
    axisVocabularies = {
        @AxisVocabulary(axis = DispositionAxis.CONFLICT_MODE,
                       uri = "urn:casehub:vocab:thomas-kilmann")
    })
public interface WeightedDispositionAgent {}
