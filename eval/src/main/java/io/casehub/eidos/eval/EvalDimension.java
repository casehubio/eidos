package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.util.EnumSet;
import java.util.Set;

public enum EvalDimension {
    SECOND_PERSON,    // "you"/"your" throughout; no third-person — prose formats only
    CONCISENESS,      // no redundancy, no filler — prose formats only
    FACTUAL_FIDELITY, // nothing claimed absent from descriptor + context — all formats
    TONE,             // reads as instructions to an AI agent — prose formats only
    COMPLETENESS;     // all capabilities present with quality descriptions — A2A_CARD only

    public static Set<EvalDimension> applicableFor(final RenderFormat format) {
        return switch (format) {
            case MARKDOWN, PROSE -> EnumSet.of(SECOND_PERSON, CONCISENESS, FACTUAL_FIDELITY, TONE);
            case A2A_CARD        -> EnumSet.of(COMPLETENESS, FACTUAL_FIDELITY);
        };
    }
}
