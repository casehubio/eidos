package io.casehub.eidos.eval;

import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer.RenderedPrompt;

public record RenderCacheEntry(String caseName, String format, String content) {

    public RenderedPrompt toRenderedPrompt() {
        return new RenderedPrompt(content, RenderFormat.valueOf(format), null, null);
    }
}
