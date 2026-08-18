package io.casehub.eidos.examples;

import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.api.Discoverable;

@Identity(slot = "code-reviewer",
          provider = "anthropic",
          modelFamily = "claude-sonnet",
          briefing = "Reviews pull requests for correctness, style, and security vulnerabilities")
@Disposition(socialOrient = "direct",
             ruleFollowing = "strict",
             riskAppetite = "cautious",
             autonomy = "autonomous",
             conflictMode = "competing")
@Discoverable(capabilities = {"code-review", "security-scan", "style-check"})
public interface CodeReviewAgent {}
