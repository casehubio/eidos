package io.casehub.eidos.runtime.renderer;

import com.fasterxml.jackson.databind.node.ObjectNode;

record StageOneResult(
        ObjectNode descriptorNode,
        ObjectNode contextNode,
        String descriptorHash,
        String contextHash,
        String lookupKey
) {}
