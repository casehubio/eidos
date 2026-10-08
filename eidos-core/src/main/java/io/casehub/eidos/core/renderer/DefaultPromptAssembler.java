package io.casehub.eidos.core.renderer;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AssembledPrompt;
import io.casehub.eidos.api.PromptAssembler;
import io.casehub.eidos.api.PromptBlock;
import io.casehub.eidos.api.spi.PromptContributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultPromptAssembler implements PromptAssembler {

    private static final Logger LOG = Logger.getLogger(DefaultPromptAssembler.class.getName());

    private final BiFunction<AgentDescriptor, AgentPromptContext, String> identityRenderer;
    private final List<PromptContributor> contributors;

    public DefaultPromptAssembler(BiFunction<AgentDescriptor, AgentPromptContext, String> identityRenderer,
                                   List<PromptContributor> contributors) {
        this.identityRenderer = identityRenderer;
        this.contributors = List.copyOf(contributors);
    }

    @Override
    public AssembledPrompt assemble(AgentDescriptor descriptor, AgentPromptContext context) {
        var allBlocks = new ArrayList<PromptBlock>();

        allBlocks.add(PromptBlock.identity(identityRenderer.apply(descriptor, context)));

        for (var contributor : contributors) {
            try {
                var blocks = contributor.contribute(descriptor, context);
                if (blocks != null) {
                    allBlocks.addAll(blocks);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "PromptContributor failed, skipping: " + contributor.getClass().getName(), e);
            }
        }

        allBlocks.sort(Comparator
                .comparingInt((PromptBlock b) -> b.tier().ordinal())
                .thenComparing(PromptBlock::salience, Comparator.reverseOrder()));

        var systemParts = new ArrayList<String>();
        var userParts = new ArrayList<String>();

        for (var block : allBlocks) {
            String rendered = renderBlock(block);
            if (block.tier().isSystemPrompt()) {
                systemParts.add(rendered);
            } else {
                userParts.add(rendered);
            }
        }

        return new AssembledPrompt(
                String.join("\n\n", systemParts),
                String.join("\n\n", userParts));
    }

    private String renderBlock(PromptBlock block) {
        if (block.tag() != null) {
            return "[" + block.tag() + "]\n" + block.content();
        }
        return block.content();
    }
}
