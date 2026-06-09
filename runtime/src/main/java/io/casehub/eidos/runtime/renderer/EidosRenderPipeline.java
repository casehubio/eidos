package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.Resource;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
class EidosRenderPipeline {

    // PROMPT_TEMPLATE must be declared before TEMPLATE_HASH — static initializers run
    // in declaration order. Reversing them causes fingerprint(null) at class load:
    // NullPointerException wrapped in ExceptionInInitializerError, not a quiet wrong value.
    static final String PROMPT_TEMPLATE = """
            You are writing narrative descriptions for an AI agent's system prompt.

            Given the agent definition in JSON, produce a JSON object with prose descriptions
            for each field. Write in second person, addressing the agent directly.

            REQUIRED FIELDS (always populate):
            - identityNarrative (1-2 sentences): The agent's name, model, and version context.
            - roleNarrative (1-3 sentences): The role this agent plays and its purpose.
              If slotLabel and slotDescription are present, prefer them over the raw slot value.
              If slotVocabularyName is present, use that framework's canonical language —
              e.g., slotVocabularyName "Belbin Team Roles" → open with the Belbin archetype
              framing ("You are the team's Monitor Evaluator...").
            - capabilityNarrative (2-4 sentences): What the agent can do.
              Include inputTypes and outputTypes when present.
              For epistemicDomains, use natural language confidence:
                >= 0.7 -> "strong expertise", 0.4-0.69 -> "working knowledge", < 0.4 -> "limited familiarity".

            OPTIONAL FIELDS (use empty string "" if the source data is absent):
            - dispositionNarrative (2-3 sentences): How the agent operates across all disposition
              axes present in the payload. The disposition object contains one nested object per axis;
              each has a "value" field and optionally "label", "description", and "vocabularyName".
              Cover all axes that have values: socialOrient, ruleFollowing, riskAppetite, autonomy,
              conflictMode. When "vocabularyName" is present, use that framework's canonical language
              rather than generic phrasing — e.g., "vocabularyName: Thomas-Kilmann Conflict Modes"
              → use TKI mode language; "vocabularyName: DISC Behavioral Styles" → use DISC canonical
              phrasing. Include delegation intent if canDelegate is true.
              Use "" if no disposition is present.
            - constraintNarrative (1-2 sentences): Data handling obligations - jurisdiction
              and compliance requirements the agent must observe.
            - goalNarrative (1-3 sentences): The agent's current task and objectives.
              Include sub-goals as a natural continuation, not a bullet list.

            RULES:
            - Second person only: "You are...", "Your role is...", "You have...".
            - Plain prose. No markdown, no bullet points, no headers.
            - Be concise. Every sentence must carry information the agent needs to act on.
            - Return ONLY the JSON object. No explanation, no preamble, no code fences.""";

    private static final String TEMPLATE_HASH = fingerprint(PROMPT_TEMPLATE).substring(0, 8);

    static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                    .name("SemanticEnrichment")
                    .rootElement(JsonObjectSchema.builder()
                            .addStringProperty("identityNarrative",
                                    "Who this agent is — name, model, version context. Second person.")
                            .addStringProperty("roleNarrative",
                                    "The agent's role and purpose. Second person.")
                            .addStringProperty("capabilityNarrative",
                                    "What the agent can do, including domain confidence. Second person.")
                            .addStringProperty("dispositionNarrative",
                                    "How the agent operates across all disposition axes in the payload. " +
                                    "Each axis object carries value, optional label, optional vocabularyName. " +
                                    "Use framework canonical language when vocabularyName is present. " +
                                    "2-3 sentences. Empty string if no disposition data.")
                            .addStringProperty("constraintNarrative",
                                    "Data handling obligations. Empty string if none.")
                            .addStringProperty("goalNarrative",
                                    "Current task and objectives. Empty string if no goal.")
                            .required("identityNarrative", "roleNarrative", "capabilityNarrative",
                                    "dispositionNarrative", "constraintNarrative", "goalNarrative")
                            .build())
                    .build())
            .build();

    static final String A2A_PROMPT_TEMPLATE = """
            You are writing per-capability descriptions for an AI agent's A2A (agent-to-agent) card.

            Given the agent's capabilities in JSON, produce a JSON object with one prose description
            per declared capability. Write in second person, addressing the agent directly.

            RULES:
            - Copy the capability name exactly as given — do not paraphrase or change capitalisation.
            - Each description is 1-2 sentences. Second person ("You can...").
            - Plain prose. No markdown, no bullet points.
            - Return ONLY the JSON object. No explanation, no preamble, no code fences.
            - If no capabilities are declared, return {"capabilityNarratives": []}.""";

    static final ResponseFormat A2A_RESPONSE_FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                    .name("A2AEnrichment")
                    .rootElement(JsonObjectSchema.builder()
                            .addProperty("capabilityNarratives", JsonArraySchema.builder()
                                    .description("One entry per declared capability. Empty array [] if none.")
                                    .items(JsonObjectSchema.builder()
                                            .addStringProperty("name",
                                                    "Capability name — must match exactly as given.")
                                            .addStringProperty("description",
                                                    "1-2 sentences, second person, what this agent can do with this capability.")
                                            .required("name", "description")
                                            .build())
                                    .build())
                            .required("capabilityNarratives")
                            .build())
                    .build())
            .build();

    static final int STREAMING_TIMEOUT_SECONDS = 30;

    private final VocabularyRegistry vocab;
    private final ObjectMapper mapper;

    @Inject
    EidosRenderPipeline(final VocabularyRegistry vocab,
                        final ObjectMapper mapper) {
        this.vocab = vocab;
        this.mapper = mapper;
    }

    // ── Stage 1: payload building ─────────────────────────────────────────────

    ObjectNode buildDescriptorPayload(final AgentDescriptor descriptor) {
        final ObjectNode node = mapper.createObjectNode();
        node.put("agentId", descriptor.agentId());
        node.put("name", descriptor.name());
        addIfPresent(node, "version", descriptor.version());
        addIfPresent(node, "provider", descriptor.provider());

        // model: combined form
        final String model = combinedModel(descriptor);
        if (model != null) node.put("model", model);

        addIfPresent(node, "weightsFingerprint", descriptor.weightsFingerprint());
        node.put("slot", descriptor.slot());

        // Vocabulary-resolved slot labels and vocabulary context
        if (descriptor.slotVocabulary() != null) {
            vocab.resolve(descriptor.slotVocabulary(), descriptor.slot()).ifPresent(term -> {
                addIfPresent(node,  "slotLabel",       term.label());
                addIfNonBlank(node, "slotDescription", term.description());
            });
            vocab.vocabularyMetadata(descriptor.slotVocabulary()).ifPresent(meta -> {
                addIfNonBlank(node, "slotVocabularyName",        meta.name());
                addIfNonBlank(node, "slotVocabularyDescription", meta.description());
            });
        }

        // Capabilities — include name, qualityHint, latencyHintP50Ms, inputTypes, outputTypes,
        // epistemicDomains. Excluded: costHint (operational), tags (routing labels).
        if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
            final ArrayNode capsArray = node.putArray("capabilities");
            for (final AgentCapability cap : descriptor.capabilities()) {
                final ObjectNode capNode = capsArray.addObject();
                capNode.put("name", cap.name());
                if (cap.qualityHint() != null)       capNode.put("qualityHint", cap.qualityHint());
                if (cap.latencyHintP50Ms() != null)  capNode.put("latencyHintP50Ms", cap.latencyHintP50Ms());
                if (cap.inputTypes() != null && !cap.inputTypes().isEmpty()) {
                    final ArrayNode arr = capNode.putArray("inputTypes");
                    cap.inputTypes().forEach(arr::add);
                }
                if (cap.outputTypes() != null && !cap.outputTypes().isEmpty()) {
                    final ArrayNode arr = capNode.putArray("outputTypes");
                    cap.outputTypes().forEach(arr::add);
                }
                if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
                    final ObjectNode domains = capNode.putObject("epistemicDomains");
                    cap.epistemicDomains().forEach(domains::put);
                }
            }
        }

        // Disposition — per-axis nested objects with vocabulary context
        if (descriptor.disposition() != null) {
            final AgentDisposition d = descriptor.disposition();
            final ObjectNode dispNode = node.putObject("disposition");
            for (DispositionAxis axis : DispositionAxis.values()) {
                d.get(axis).ifPresent(rawValue -> {
                    final ObjectNode axisNode = dispNode.putObject(axisJsonKey(axis));
                    axisNode.put("value", rawValue);
                    descriptor.vocabUriForAxis(axis).ifPresent(uri -> {
                        vocab.resolve(uri, rawValue).ifPresent(term -> {
                            addIfNonBlank(axisNode, "label",       term.label());
                            addIfNonBlank(axisNode, "description", term.description());
                        });
                        vocab.vocabularyMetadata(uri).ifPresent(meta -> {
                            addIfNonBlank(axisNode, "vocabularyName",        meta.name());
                            addIfNonBlank(axisNode, "vocabularyDescription", meta.description());
                        });
                    });
                });
            }
            dispNode.put("canDelegate", d.delegation());
        }

        addIfPresent(node, "jurisdiction",       descriptor.jurisdiction());
        addIfPresent(node, "dataHandlingPolicy", descriptor.dataHandlingPolicy());

        return node;
    }

    ObjectNode buildContextPayload(final AgentPromptContext context) {
        final ObjectNode node = mapper.createObjectNode();
        context.goal().ifPresent(goal -> {
            final ObjectNode goalNode = node.putObject("goal");
            goalNode.put("description", goal.description());
            if (!goal.subGoals().isEmpty()) {
                final ArrayNode subGoals = goalNode.putArray("subGoals");
                goal.subGoals().forEach(subGoals::add);
            }
            addIfPresent(goalNode, "caseRef", goal.caseRef());
        });
        // situationalContext and resources are structural-only (not sent to LLM) but
        // they affect the rendered output, so they must be part of the context hash
        // to ensure cache correctness.
        if (context.situationalContext() != null) {
            node.put("situationalContext", context.situationalContext());
        }
        if (!context.resources().isEmpty()) {
            final ArrayNode resourcesArray = node.putArray("resources");
            for (final Resource r : context.resources()) {
                final ObjectNode rNode = resourcesArray.addObject();
                rNode.put("uri", r.uri());
                addIfPresent(rNode, "label", r.label());
                addIfPresent(rNode, "type", r.type());
            }
        }
        return node;
    }

    /** Goal-only payload for the LLM call. situationalContext and resources are excluded — structural-only fields rendered in Stage 3. */
    ObjectNode buildLlmPayload(final ObjectNode descriptorNode,
                               final ObjectNode contextNode) {
        final ObjectNode full = descriptorNode.deepCopy();
        if (contextNode.has("goal")) {
            full.set("goal", contextNode.get("goal").deepCopy());
        }
        return full;
    }

    // ── Stage 1: build + fingerprint ─────────────────────────────────────────

    StageOneResult buildStage1(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final ObjectNode descriptorNode = buildDescriptorPayload(descriptor);
        final ObjectNode contextNode    = buildContextPayload(context);
        final String descriptorHash     = fingerprint(descriptorNode.toString());
        final String contextHash        = fingerprint(contextNode.toString());
        final String key                = cacheKey(descriptorHash, contextHash, context.format());
        return new StageOneResult(descriptorNode, contextNode, descriptorHash, contextHash, key);
    }

    // ── Cache utilities ──────────────────────────────────────────────────────

    String cacheKey(final String descriptorHash, final String contextHash,
                    final RenderFormat format) {
        return descriptorHash + ":" + contextHash + ":" + format.name() + ":" + TEMPLATE_HASH;
    }

    // ── Stage 2 predicate ────────────────────────────────────────────────────

    static boolean usesEnrichment(final RenderFormat format) {
        return switch (format) {
            case MARKDOWN, PROSE -> true;
            case A2A_CARD        -> false;
        };
    }

    // ── Stage 3: format assembly ──────────────────────────────────────────────

    RenderedPrompt assemble(final StageOneResult s1,
                             final Optional<SemanticEnrichment> enrichment,
                             final Optional<A2AEnrichment> a2aEnrichment,
                             final AgentDescriptor descriptor,
                             final AgentPromptContext context) {
        final String content = switch (context.format()) {
            case MARKDOWN  -> assembleMarkdown(enrichment, descriptor, context);
            case PROSE     -> assembleProse(enrichment, descriptor, context);
            case A2A_CARD  -> assembleA2aCard(a2aEnrichment, descriptor);
        };
        return new RenderedPrompt(content, context.format(), s1.descriptorHash(), s1.contextHash());
    }

    // ── Format-specific assembly ─────────────────────────────────────────────

    private String assembleMarkdown(final Optional<SemanticEnrichment> enrichment,
                                     final AgentDescriptor descriptor,
                                     final AgentPromptContext context) {
        final var sb = new StringBuilder();

        // Header — always structural
        sb.append("# ").append(descriptor.name()).append("\n");
        sb.append("**Agent ID:** ").append(descriptor.agentId());
        final String model = combinedModel(descriptor);
        if (model != null)                   sb.append("  **Model:** ").append(model);
        if (descriptor.provider() != null)   sb.append("  **Provider:** ").append(descriptor.provider());
        sb.append("\n");

        if (enrichment.isPresent()) {
            final SemanticEnrichment e = enrichment.get();
            sb.append("\n").append(e.identityNarrative()).append("\n");
            sb.append("\n## Role\n").append(e.roleNarrative()).append("\n");
            sb.append("\n## Capabilities\n").append(e.capabilityNarrative()).append("\n");
            e.dispositionNarrative().ifPresent(d ->
                sb.append("\n## How You Operate\n").append(d).append("\n"));
            e.constraintNarrative().ifPresent(c ->
                sb.append("\n## Data Handling\n").append(c).append("\n"));
            e.goalNarrative().ifPresent(g ->
                sb.append("\n## Current Goal\n").append(g).append("\n"));
        } else {
            assembleMarkdownStructural(sb, descriptor, context);
        }

        // Resources — always structural
        if (!context.resources().isEmpty()) {
            sb.append("\n## Resources\n");
            for (final Resource r : context.resources()) {
                sb.append("- **").append(r.label() != null ? r.label() : r.uri()).append("**: ").append(r.uri());
                if (r.type() != null) sb.append(" (").append(r.type()).append(")");
                sb.append("\n");
            }
        }

        // Situational context — always structural
        if (context.situationalContext() != null) {
            sb.append("\n## Context\n").append(context.situationalContext()).append("\n");
        }

        return sb.toString().trim();
    }

    private void assembleMarkdownStructural(final StringBuilder sb,
                                             final AgentDescriptor descriptor,
                                             final AgentPromptContext context) {
        // Role — deliberate heading change from ## {slot_label} to ## Role
        // (see spec behavioral delta note)
        if (descriptor.slot() != null) {
            sb.append("\n## Role\n");
            if (descriptor.slotVocabulary() != null) {
                vocab.resolve(descriptor.slotVocabulary(), descriptor.slot()).ifPresentOrElse(
                    term -> {
                        if (term.label() != null)       sb.append(term.label()).append("\n");
                        if (term.description() != null) sb.append(term.description()).append("\n");
                    },
                    () -> sb.append(descriptor.slot()).append("\n")
                );
            } else {
                sb.append(descriptor.slot()).append("\n");
            }
        }

        // Capabilities
        if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
            sb.append("\n## Capabilities\n");
            for (final AgentCapability cap : descriptor.capabilities()) {
                sb.append("- **").append(cap.name()).append("**");
                if (cap.qualityHint() != null) sb.append(": quality ").append(cap.qualityHint());
                if (cap.latencyHintP50Ms() != null)
                    sb.append(", p50 ").append(cap.latencyHintP50Ms()).append("ms");
                sb.append("\n");
                if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
                    sb.append("  Domains: ").append(cap.epistemicDomains()).append("\n");
                }
            }
        }

        // Disposition
        if (descriptor.disposition() != null) {
            final AgentDisposition d = descriptor.disposition();
            sb.append("\n## How You Operate\n");
            for (DispositionAxis axis : DispositionAxis.values()) {
                d.get(axis).ifPresent(raw ->
                    sb.append("- ").append(axisLabel(axis)).append(": ")
                      .append(resolveAxisDisplay(axis, raw, descriptor)).append("\n"));
            }
            sb.append("- Can delegate: ").append(d.delegation() ? "yes" : "no").append("\n");
        }

        // Data handling
        if (descriptor.jurisdiction() != null || descriptor.dataHandlingPolicy() != null) {
            sb.append("\n## Data Handling\n");
            if (descriptor.jurisdiction() != null)
                sb.append("Jurisdiction: ").append(descriptor.jurisdiction()).append("\n");
            if (descriptor.dataHandlingPolicy() != null)
                sb.append("Policy: ").append(descriptor.dataHandlingPolicy()).append("\n");
        }

        // Goal
        context.goal().ifPresent(goal -> {
            sb.append("\n## Current Goal\n");
            sb.append(goal.description()).append("\n");
            if (!goal.subGoals().isEmpty()) {
                goal.subGoals().forEach(sub -> sb.append("- ").append(sub).append("\n"));
            }
            if (goal.caseRef() != null) sb.append("Case: ").append(goal.caseRef()).append("\n");
        });
    }

    private String assembleProse(final Optional<SemanticEnrichment> enrichment,
                                  final AgentDescriptor descriptor,
                                  final AgentPromptContext context) {
        final var sb = new StringBuilder();

        if (enrichment.isPresent()) {
            final SemanticEnrichment e = enrichment.get();
            sb.append(e.identityNarrative()).append(" ").append(e.roleNarrative()).append("\n");
            sb.append("\n").append(e.capabilityNarrative()).append("\n");
            e.dispositionNarrative().ifPresent(d -> sb.append("\n").append(d).append("\n"));
            e.constraintNarrative().ifPresent(c -> sb.append("\n").append(c).append("\n"));
            e.goalNarrative().ifPresent(g -> sb.append("\n").append(g).append("\n"));
        } else {
            // Structural PROSE — dense prose, no headers
            sb.append(descriptor.name());
            if (descriptor.slot() != null) sb.append(", ").append(descriptor.slot());
            sb.append(".");
            if (descriptor.version() != null) sb.append(" Version ").append(descriptor.version()).append(".");
            sb.append("\n");

            if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
                sb.append("\nCapabilities: ");
                final var names = descriptor.capabilities().stream()
                        .map(AgentCapability::name)
                        .collect(Collectors.joining(", "));
                sb.append(names).append(".\n");
            }

            if (descriptor.disposition() != null) {
                final AgentDisposition d = descriptor.disposition();
                sb.append("\nOperating style:");
                for (DispositionAxis axis : DispositionAxis.values()) {
                    d.get(axis).ifPresent(raw ->
                        sb.append(" ").append(axisLabel(axis)).append(": ")
                          .append(resolveAxisDisplay(axis, raw, descriptor)).append("."));
                }
                sb.append(" Can delegate: ").append(d.delegation() ? "yes" : "no").append(".\n");
            }

            context.goal().ifPresent(goal -> {
                sb.append("\nGoal: ").append(goal.description()).append(".\n");
                if (!goal.subGoals().isEmpty()) {
                    sb.append("Sub-goals: ").append(String.join(", ", goal.subGoals())).append(".\n");
                }
            });
        }

        // Resources — always structural
        if (!context.resources().isEmpty()) {
            sb.append("\nResources: ");
            final var resources = context.resources().stream()
                    .map(r -> (r.label() != null ? r.label() : r.uri()) + " (" + r.uri() + ")")
                    .collect(Collectors.joining(", "));
            sb.append(resources).append(".\n");
        }

        if (context.situationalContext() != null) {
            sb.append("\n").append(context.situationalContext()).append("\n");
        }

        return sb.toString().trim();
    }

    private String assembleA2aCard(final Optional<A2AEnrichment> enrichment,
                                    final AgentDescriptor descriptor) {
        final ObjectNode card = mapper.createObjectNode();
        card.put("name", descriptor.name());
        card.put("agentId", descriptor.agentId());
        addIfPresent(card, "version", descriptor.version());

        if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
            final Map<String, String> descriptionByName = enrichment
                .map(e -> e.capabilityNarratives().stream()
                    .collect(Collectors.toMap(
                        A2AEnrichment.CapabilityNarrative::name,
                        A2AEnrichment.CapabilityNarrative::description,
                        (a, b) -> a)))
                .orElse(Map.of());

            final ArrayNode capsArray = card.putArray("capabilities");
            for (final AgentCapability cap : descriptor.capabilities()) {
                final ObjectNode capNode = capsArray.addObject();
                capNode.put("name", cap.name());
                if (cap.qualityHint() != null) capNode.put("qualityHint", cap.qualityHint());
                final String desc = descriptionByName.get(cap.name());
                if (desc != null) capNode.put("description", desc);
            }
        }

        try {
            return mapper.writeValueAsString(card);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("A2A card serialization failed", ex);
        }
    }

    // ── Shared utilities ───────────────────────────────────────────────────────

    private static String combinedModel(final AgentDescriptor descriptor) {
        if (descriptor.modelFamily() != null && descriptor.modelVersion() != null)
            return descriptor.modelFamily() + "/" + descriptor.modelVersion();
        if (descriptor.modelFamily() != null) return descriptor.modelFamily();
        return descriptor.modelVersion();
    }

    private static void addIfPresent(final ObjectNode node, final String key, final String value) {
        if (value != null) node.put(key, value);
    }

    private static void addIfNonBlank(final ObjectNode node, final String key, final String value) {
        if (value != null && !value.isEmpty()) node.put(key, value);
    }

    private static String axisJsonKey(final DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> "socialOrient";
            case RULE_FOLLOWING     -> "ruleFollowing";
            case RISK_APPETITE      -> "riskAppetite";
            case AUTONOMY           -> "autonomy";
            case CONFLICT_MODE      -> "conflictMode";
        };
    }

    private static String axisLabel(final DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> "Social orientation";
            case RULE_FOLLOWING     -> "Rule following";
            case RISK_APPETITE      -> "Risk appetite";
            case AUTONOMY           -> "Autonomy";
            case CONFLICT_MODE      -> "Conflict mode";
        };
    }

    private String resolveAxisDisplay(final DispositionAxis axis, final String raw,
                                       final AgentDescriptor descriptor) {
        final Optional<String> vocabUri = descriptor.vocabUriForAxis(axis);
        final String label = vocabUri
            .flatMap(uri -> vocab.resolve(uri, raw))
            .map(VocabularyTerm::label)
            .filter(l -> !l.isEmpty())
            .orElse(raw);
        final String vocabName = vocabUri
            .flatMap(uri -> vocab.vocabularyMetadata(uri))
            .map(VocabularyMetadata::name)
            .filter(n -> !n.isEmpty())
            .orElse(null);
        return vocabName != null ? label + " (" + vocabName + ")" : label;
    }

    /**
     * Returns a 16-char hex prefix of the SHA-256 hash of {@code input}.
     * 16 hex chars = 64 bits. Birthday-bound collision probability is negligible
     * for the number of descriptors and contexts in a single deployment.
     * Not a full SHA-256 hash — use only for cache keys and display fingerprints,
     * not for security-sensitive purposes.
     */
    static String fingerprint(final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
