package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:model-tier",
                    name = "Model Tier", version = "1.0",
                    description = "LLM model capability tiers for identity-scoped routing. Linear subsumption: FLAGSHIP can satisfy any STANDARD requirement. EMBEDDING is a separate modality.")
public enum ModelTierTerm implements VocabularyTerm {

    FLAGSHIP("flagship", "Flagship",
             "Highest-capability models with advanced reasoning, code generation, and complex instruction following") {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(STANDARD);
        }
    },
    STANDARD("standard", "Standard",
             "Balanced models suitable for most production tasks — good quality at moderate cost") {
        @Override public List<VocabularyTerm> specializes() {
            return List.of(FAST);
        }
    },
    FAST("fast", "Fast",
         "Low-latency models optimized for speed and throughput over reasoning depth"),
    EMBEDDING("embedding", "Embedding",
              "Embedding models for vector representations — different modality, not a compute tier");

    public static final String URI = "urn:casehub:vocab:model-tier";

    private final String value, label, description;

    ModelTierTerm(String value, String label, String description) {
        this.value       = value;
        this.label       = label;
        this.description = description;
    }

    @Override public String value()       { return value; }
    @Override public String label()       { return label; }
    @Override public String description() { return description; }
}
