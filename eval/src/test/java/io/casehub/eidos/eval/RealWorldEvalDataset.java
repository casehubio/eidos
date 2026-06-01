package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.util.List;
import java.util.stream.Stream;

class RealWorldEvalDataset {

    static List<ProfiledEvalCase> all() {
        return new AgentProfileLoader().load().stream()
            .flatMap(profile -> Stream.of(
                profileCase(profile, RenderFormat.MARKDOWN),
                profileCase(profile, RenderFormat.PROSE)
            ))
            .toList();
    }

    private static ProfiledEvalCase profileCase(final AgentProfile profile,
                                                 final RenderFormat format) {
        final AgentPromptContext ctx = profile.evalGoal() != null
            ? AgentPromptContext.forFormat(format).withGoal(profile.evalGoal())
            : AgentPromptContext.forFormat(format);
        return new ProfiledEvalCase(
            profile.name() + "-" + format.name().toLowerCase(),
            profile.descriptor(),
            ctx,
            profile
        );
    }
}
