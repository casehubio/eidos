package io.casehub.eidos.examples;

import io.casehub.eidos.annotations.Identity;

@Identity(slot = "triage",
          briefing = "Routes incoming support tickets to the right team based on urgency and topic")
public interface CustomerSupportTriage {}
