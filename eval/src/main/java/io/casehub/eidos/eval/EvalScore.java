package io.casehub.eidos.eval;

public record EvalScore(int score, String reasoning) {
    public EvalScore {
        if (score < 0 || score > 5) throw new IllegalArgumentException("score must be 0–5, was " + score);
    }
}
