package io.casehub.eidos.api;

import java.util.Map;

public record PersonalityInput(
        String mbtiType,
        String enneagramType,
        boolean hasExplicitProfile,
        Map<DispositionAxis, String> explicitAxes) {

    public PersonalityInput {
        mbtiType = mbtiType != null ? mbtiType : "";
        enneagramType = enneagramType != null ? enneagramType : "";
        explicitAxes = explicitAxes != null ? Map.copyOf(explicitAxes) : Map.of();
    }
}
