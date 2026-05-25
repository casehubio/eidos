package io.casehub.eidos.runtime.renderer;

import dev.langchain4j.model.chat.ChatModel;
import io.casehub.eidos.api.*;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@DefaultBean
@ApplicationScoped
public class ClaudeMarkdownRenderer implements SystemPromptRenderer {

    private final ChatModel llm;
    private final VocabularyRegistry vocab;

    @Inject
    public ClaudeMarkdownRenderer(
            @Any final Instance<ChatModel> llm,
            final VocabularyRegistry vocab) {
        // ChatModel must be @ApplicationScoped (or broader). A @Dependent-scoped
        // ChatModel obtained via Instance.get() would leak because ClaudeMarkdownRenderer
        // is @ApplicationScoped and holds the bean reference without destroying it.
        // Quarkus LangChain4j always registers ChatModel as @ApplicationScoped, so this
        // is safe in practice. Custom implementations must declare an explicit scope.
        this.llm = llm.isResolvable() ? llm.get() : null;
        this.vocab = vocab;
    }

    /** Package-private constructor for pure Java tests. */
    ClaudeMarkdownRenderer(final ChatModel llm, final VocabularyRegistry vocab) {
        this.llm = llm;
        this.vocab = vocab;
    }

    @Override
    public RenderedPrompt render(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final String content;
        if (llm != null) {
            content = renderWithLlm(descriptor, context);
        } else {
            content = renderStructural(descriptor, context);
        }
        return new RenderedPrompt(
                content,
                context.format(),
                sha256(descriptor.toString()),
                sha256(context.toString())
        );
    }

    private String renderWithLlm(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final var yaml = toYaml(descriptor, context);
        final var prompt = """
                You are generating a system prompt for an LLM agent.
                Below is the agent's structured definition in YAML.
                Generate a system prompt optimised for LLM consumption — use whatever
                structure is clearest and most concise. Do not optimise for human
                readability. Every token should carry information the agent needs to act on.

                """ + yaml;
        return llm.chat(prompt);
    }

    private String renderStructural(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final var sb = new StringBuilder();

        // Header
        sb.append("# ").append(descriptor.name()).append("\n");
        sb.append("**Agent ID:** ").append(descriptor.agentId());
        if (descriptor.modelFamily() != null) {
            sb.append("  **Model:** ").append(descriptor.modelFamily());
            if (descriptor.modelVersion() != null) {
                sb.append("/").append(descriptor.modelVersion());
            }
        }
        if (descriptor.provider() != null) {
            sb.append("  **Provider:** ").append(descriptor.provider());
        }
        sb.append("\n");

        // Slot section — heading uses vocabulary label if available, else raw slot value;
        // body uses vocabulary term description if available.
        if (descriptor.slot() != null) {
            String heading = descriptor.slot();
            String termDescription = null;
            if (descriptor.slotVocabulary() != null) {
                final var term = vocab.resolve(descriptor.slotVocabulary(), descriptor.slot());
                if (term.isPresent()) {
                    if (term.get().label() != null) heading = term.get().label();
                    termDescription = term.get().description();
                }
            }
            sb.append("\n## ").append(heading).append("\n");
            if (termDescription != null) sb.append(termDescription).append("\n");
        }

        // Capabilities
        if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
            sb.append("\n## Capabilities\n");
            for (final var cap : descriptor.capabilities()) {
                sb.append("- **").append(cap.name()).append("**");
                if (cap.qualityHint() != null) sb.append(": quality ").append(cap.qualityHint());
                if (cap.latencyHintP50Ms() != null) sb.append(", p50 ").append(cap.latencyHintP50Ms()).append("ms");
                sb.append("\n");
                if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
                    sb.append("  Domains: ").append(cap.epistemicDomains()).append("\n");
                }
            }
        }

        // Disposition
        if (descriptor.disposition() != null) {
            final var d = descriptor.disposition();
            sb.append("\n## Disposition\n");
            if (d.socialOrient() != null) sb.append("- Social orientation: ").append(d.socialOrient()).append("\n");
            if (d.ruleFollowing() != null) sb.append("- Rule following: ").append(d.ruleFollowing()).append("\n");
            if (d.riskAppetite() != null) sb.append("- Risk appetite: ").append(d.riskAppetite()).append("\n");
            if (d.autonomy() != null) sb.append("- Autonomy: ").append(d.autonomy()).append("\n");
            sb.append("- Can delegate: ").append(d.delegation() ? "yes" : "no").append("\n");
        }

        // Goal
        context.goal().ifPresent(goal -> {
            sb.append("\n## Goal\n");
            sb.append(goal.description()).append("\n");
            if (!goal.subGoals().isEmpty()) {
                for (final var sub : goal.subGoals()) {
                    sb.append("- ").append(sub).append("\n");
                }
            }
            if (goal.caseRef() != null) {
                sb.append("Case: ").append(goal.caseRef()).append("\n");
            }
        });

        // Resources
        if (!context.resources().isEmpty()) {
            sb.append("\n## Resources\n");
            for (final var r : context.resources()) {
                sb.append("- **").append(r.label()).append("**: ").append(r.uri());
                if (r.type() != null) sb.append(" (").append(r.type()).append(")");
                sb.append("\n");
            }
        }

        // Situational context
        if (context.situationalContext() != null) {
            sb.append("\n## Context\n").append(context.situationalContext()).append("\n");
        }

        // Data handling
        if (descriptor.jurisdiction() != null || descriptor.dataHandlingPolicy() != null) {
            sb.append("\n## Data Handling\n");
            if (descriptor.jurisdiction() != null) {
                sb.append("Jurisdiction: ").append(descriptor.jurisdiction()).append("\n");
            }
            if (descriptor.dataHandlingPolicy() != null) {
                sb.append("Policy: ").append(descriptor.dataHandlingPolicy()).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private String toYaml(final AgentDescriptor descriptor, final AgentPromptContext context) {
        final var sb = new StringBuilder();
        sb.append("agent:\n");
        yamlField(sb, 2, "id", descriptor.agentId());
        yamlField(sb, 2, "name", descriptor.name());
        if (descriptor.modelFamily() != null && descriptor.modelVersion() != null) {
            yamlField(sb, 2, "model", descriptor.modelFamily() + "/" + descriptor.modelVersion());
        } else {
            yamlField(sb, 2, "model",
                descriptor.modelFamily() != null ? descriptor.modelFamily() : descriptor.modelVersion());
        }
        yamlField(sb, 2, "provider", descriptor.provider());
        yamlField(sb, 2, "version", descriptor.version());
        yamlField(sb, 2, "slot", descriptor.slot());

        if (descriptor.capabilities() != null && !descriptor.capabilities().isEmpty()) {
            sb.append("  capabilities:\n");
            for (final var cap : descriptor.capabilities()) {
                sb.append("    - name: ").append(cap.name()).append("\n");
                if (cap.qualityHint() != null) yamlField(sb, 6, "qualityHint", cap.qualityHint().toString());
                if (cap.latencyHintP50Ms() != null) yamlField(sb, 6, "latencyHintP50Ms", cap.latencyHintP50Ms().toString());
                if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
                    sb.append("      epistemicDomains:\n");
                    cap.epistemicDomains().forEach((domain, conf) ->
                            sb.append("        ").append(domain).append(": ").append(conf).append("\n"));
                }
            }
        }

        if (descriptor.disposition() != null) {
            final var d = descriptor.disposition();
            sb.append("  disposition:\n");
            yamlField(sb, 4, "socialOrient", d.socialOrient());
            yamlField(sb, 4, "ruleFollowing", d.ruleFollowing());
            yamlField(sb, 4, "riskAppetite", d.riskAppetite());
            yamlField(sb, 4, "autonomy", d.autonomy());
            sb.append("    canDelegate: ").append(d.delegation()).append("\n");
        }

        sb.append("context:\n");
        context.goal().ifPresent(goal -> {
            sb.append("  goal:\n");
            yamlField(sb, 4, "description", goal.description());
            if (!goal.subGoals().isEmpty()) {
                sb.append("    subGoals:\n");
                goal.subGoals().forEach(sub -> sb.append("      - ").append(sub).append("\n"));
            }
            yamlField(sb, 4, "caseRef", goal.caseRef());
        });

        if (!context.resources().isEmpty()) {
            sb.append("  resources:\n");
            for (final var r : context.resources()) {
                sb.append("    - uri: ").append(r.uri()).append("\n");
                yamlField(sb, 6, "label", r.label());
                yamlField(sb, 6, "type", r.type());
            }
        }

        yamlField(sb, 2, "situationalContext", context.situationalContext());
        return sb.toString();
    }

    private static void yamlField(final StringBuilder sb, final int indent, final String key, final String value) {
        if (value == null) return;
        // Single-quote scalars handle colons, hashes, and other YAML special characters
        // without escape sequences; single quotes inside the value are doubled.
        sb.append(" ".repeat(indent)).append(key).append(": '")
                .append(value.replace("'", "''"))
                .append("'\n");
    }

    private static String sha256(final String input) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
