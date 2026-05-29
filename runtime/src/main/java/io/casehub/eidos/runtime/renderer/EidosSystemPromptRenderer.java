package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.RenderedPromptCache;
import io.casehub.eidos.api.Resource;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.langchain4j.model.chat.ChatModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@DefaultBean
@ApplicationScoped
public class EidosSystemPromptRenderer implements SystemPromptRenderer {

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
            - capabilityNarrative (2-4 sentences): What the agent can do.
              Include inputTypes and outputTypes when present.
              For epistemicDomains, use natural language confidence:
                >= 0.7 -> "strong expertise", 0.4-0.69 -> "working knowledge", < 0.4 -> "limited familiarity".

            OPTIONAL FIELDS (use empty string "" if the source data is absent):
            - dispositionNarrative (1-2 sentences): How the agent operates - autonomy,
              rule-following orientation, delegation authority.
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

    private final ChatModel llm;
    private final VocabularyRegistry vocab;
    private final RenderedPromptCache cache;
    private final ObjectMapper mapper;
    private final SemanticEnrichmentStep enrichmentStep;
    private final A2ASemanticEnrichmentStep a2aEnrichmentStep;

    @Inject
    public EidosSystemPromptRenderer(
            @Any final Instance<ChatModel> llm,
            final VocabularyRegistry vocab,
            final RenderedPromptCache cache,
            final ObjectMapper mapper) {
        // ChatModel must be @ApplicationScoped (or broader). A @Dependent-scoped ChatModel
        // obtained via Instance.get() would leak. Quarkus LangChain4j always registers
        // ChatModel as @ApplicationScoped, so this is safe in practice.
        this.llm = llm.isResolvable() ? llm.get() : null;
        this.vocab = vocab;
        this.cache = cache;
        this.mapper = mapper;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    /** Package-private constructor for pure-Java tests — no CDI required. */
    EidosSystemPromptRenderer(final ChatModel llm, final VocabularyRegistry vocab,
                           final RenderedPromptCache cache, final ObjectMapper mapper) {
        this.llm = llm;
        this.vocab = vocab;
        this.cache = cache;
        this.mapper = mapper;
        this.enrichmentStep = new SemanticEnrichmentStep(mapper);
        this.a2aEnrichmentStep = new A2ASemanticEnrichmentStep(mapper);
    }

    @Override
    public RenderedPrompt render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final ObjectNode descriptorNode = buildDescriptorPayload(descriptor);
        final ObjectNode contextNode    = buildContextPayload(context);

        final String descriptorHash = fingerprint(descriptorNode.toString());
        final String contextHash    = fingerprint(contextNode.toString());
        final String cacheKey       = descriptorHash + ":" + contextHash + ":"
                                    + context.format().name() + ":" + TEMPLATE_HASH;

        final Optional<RenderedPrompt> cached = cache.get(cacheKey);
        if (cached.isPresent()) return cached.get();

        // Stage 2a: optional semantic enrichment
        Optional<SemanticEnrichment> enrichment = Optional.empty();
        if (llm != null && usesEnrichment(context.format())) {
            final ObjectNode llmPayload = buildLlmPayload(descriptorNode, contextNode);
            enrichment = enrichmentStep.enrich(llm, llmPayload);
        }

        // Stage 2b: A2A enrichment — descriptor-only payload, separate schema
        Optional<A2AEnrichment> a2aEnrichment = Optional.empty();
        if (context.format() == RenderFormat.A2A_CARD && llm != null) {
            a2aEnrichment = a2aEnrichmentStep.enrich(llm, descriptorNode);
        }

        // Stage 3: format-specific assembly
        final String content = assemble(enrichment, a2aEnrichment, descriptor, context);
        final RenderedPrompt result = new RenderedPrompt(content, context.format(),
                                                         descriptorHash, contextHash);
        cache.put(cacheKey, result);
        return result;
    }

    // ── Stage 2 predicate ────────────────────────────────────────────────────

    private static boolean usesEnrichment(final RenderFormat format) {
        return switch (format) {
            case CLAUDE_MD, OPENAI_SYSTEM, GEMINI -> true;
            case A2A_CARD                          -> false;
        };
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

        // Vocabulary-resolved slot labels
        if (descriptor.slotVocabulary() != null) {
            vocab.resolve(descriptor.slotVocabulary(), descriptor.slot()).ifPresent(term -> {
                addIfPresent(node, "slotLabel", term.label());
                addIfPresent(node, "slotDescription", term.description());
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

        // Disposition
        if (descriptor.disposition() != null) {
            final AgentDisposition d = descriptor.disposition();
            final ObjectNode dispNode = node.putObject("disposition");
            addIfPresent(dispNode, "socialOrient",   d.socialOrient());
            addIfPresent(dispNode, "ruleFollowing",  d.ruleFollowing());
            addIfPresent(dispNode, "riskAppetite",   d.riskAppetite());
            addIfPresent(dispNode, "autonomy",        d.autonomy());
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
    private ObjectNode buildLlmPayload(final ObjectNode descriptorNode,
                                        final ObjectNode contextNode) {
        final ObjectNode full = descriptorNode.deepCopy();
        if (contextNode.has("goal")) {
            full.set("goal", contextNode.get("goal").deepCopy());
        }
        return full;
    }


    // ── Stage 3: format assembly ──────────────────────────────────────────────

    private String assemble(final Optional<SemanticEnrichment> enrichment,
                             final Optional<A2AEnrichment> a2aEnrichment,
                             final AgentDescriptor descriptor,
                             final AgentPromptContext context) {
        return switch (context.format()) {
            case CLAUDE_MD     -> assembleClaudeMarkdown(enrichment, descriptor, context);
            case OPENAI_SYSTEM -> assembleOpenAiSystem(enrichment, descriptor, context);
            case A2A_CARD      -> assembleA2aCard(a2aEnrichment, descriptor);
            case GEMINI        -> assembleGemini(enrichment, descriptor, context);
        };
    }

    private String assembleClaudeMarkdown(final Optional<SemanticEnrichment> enrichment,
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
            assembleClaudeMarkdownStructural(sb, descriptor, context);
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

    private void assembleClaudeMarkdownStructural(final StringBuilder sb,
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
            if (d.socialOrient() != null)  sb.append("- Social orientation: ").append(d.socialOrient()).append("\n");
            if (d.ruleFollowing() != null) sb.append("- Rule following: ").append(d.ruleFollowing()).append("\n");
            if (d.riskAppetite() != null)  sb.append("- Risk appetite: ").append(d.riskAppetite()).append("\n");
            if (d.autonomy() != null)      sb.append("- Autonomy: ").append(d.autonomy()).append("\n");
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

    private String assembleOpenAiSystem(final Optional<SemanticEnrichment> enrichment,
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
            // Structural OPENAI_SYSTEM — dense prose, no headers
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
                if (d.ruleFollowing() != null) sb.append(" ").append(d.ruleFollowing()).append(" rule-following.");
                if (d.autonomy() != null)      sb.append(" Autonomy: ").append(d.autonomy()).append(".");
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

    private String assembleGemini(final Optional<SemanticEnrichment> enrichment,
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
            assembleGeminiStructural(sb, descriptor, context);
        }

        // Resources — label(uri) format, no space before paren (explicit delta from OPENAI_SYSTEM)
        if (!context.resources().isEmpty()) {
            sb.append("\nResources: ");
            final var resources = context.resources().stream()
                    .map(r -> (r.label() != null ? r.label() : r.uri()) + "(" + r.uri() + ")")
                    .collect(Collectors.joining(", "));
            sb.append(resources).append("\n");
        }

        if (context.situationalContext() != null) {
            sb.append("\n").append(context.situationalContext()).append("\n");
        }

        return sb.toString().trim();
    }

    private void assembleGeminiStructural(final StringBuilder sb,
                                           final AgentDescriptor descriptor,
                                           final AgentPromptContext context) {
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
            if (d.ruleFollowing() != null) sb.append(" ").append(d.ruleFollowing()).append(" rule-following.");
            if (d.autonomy() != null)      sb.append(" Autonomy: ").append(d.autonomy()).append(".");
            sb.append(" Can delegate: ").append(d.delegation() ? "yes" : "no").append(".\n");
        }

        context.goal().ifPresent(goal -> {
            sb.append("\nGoal: ").append(goal.description()).append(".\n");
            if (!goal.subGoals().isEmpty()) {
                sb.append("Sub-goals: ").append(String.join(", ", goal.subGoals())).append(".\n");
            }
        });
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
