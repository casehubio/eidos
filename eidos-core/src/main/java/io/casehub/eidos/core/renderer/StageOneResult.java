package io.casehub.eidos.core.renderer;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record StageOneResult(
        ObjectNode descriptorNode,
        ObjectNode contextNode,
        String descriptorHash,
        String contextHash,
        String lookupKey
) {}
