package io.casehub.eidos.examples;

import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.annotations.Disposition;

@Identity(slot = "document-analyst",
          briefing = "Analyses documents and extracts key findings")
@Disposition(socialOrient = "collaborative",
             ruleFollowing = "moderate",
             riskAppetite = "cautious")
public interface DocumentAnalyst {}
