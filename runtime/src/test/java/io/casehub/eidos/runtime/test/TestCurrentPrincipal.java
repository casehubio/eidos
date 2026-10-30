package io.casehub.eidos.runtime.test;

import io.casehub.platform.api.identity.CurrentPrincipal;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

@DefaultBean
@ApplicationScoped
public class TestCurrentPrincipal implements CurrentPrincipal {
    @Override public String actorId() { return "test-actor"; }
    @Override public Set<String> groups() { return Set.of(); }
    @Override public String tenancyId() { return "test"; }
    @Override public boolean isCrossTenantAdmin() { return false; }
}
