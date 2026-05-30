package io.casehub.eidos.eval;

public enum EvalDimension {
    SECOND_PERSON,    // "you"/"your" throughout; no third-person
    CONCISENESS,      // no redundancy, no filler
    FACTUAL_FIDELITY, // nothing claimed absent from descriptor + context
    TONE              // reads as instructions to an AI agent
}
