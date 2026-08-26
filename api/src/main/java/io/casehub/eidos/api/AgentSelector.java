package io.casehub.eidos.api;

import java.util.List;

public interface AgentSelector {
    AgentSelection select(List<AgentMatch> candidates, SelectionContext context);
}
