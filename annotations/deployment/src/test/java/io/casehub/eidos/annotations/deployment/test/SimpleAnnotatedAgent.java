package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.annotations.Disposition;

@Identity(slot = "test-agent", briefing = "A test agent")
@Disposition(socialOrient = "collaborative", ruleFollowing = "strict")
public interface SimpleAnnotatedAgent {}
