package io.casehub.eidos.api;

import java.util.List;

/**
 * Notification callback for disposition profile evolution events.
 *
 * <p>Called after a successful JPAF type transition or cognitive function
 * reweighting. Implementations use this for side-effects (audit, event
 * emission) — not for persistence. Evolved state is owned by the
 * personality evolution subsystem (e.g. neocortex's PersonalityStateService),
 * not by the eidos registry.
 *
 * <p>The default bean ({@code NoOpDispositionProfileListener}) discards
 * the notification. Consumers that need to observe evolution provide
 * their own implementation.
 *
 * @see LivedDispositionSource for the read path (composed personality queries)
 */
public interface DispositionProfileListener {
    void onProfileEvolved(String agentId, String tenancyId, List<DispositionValue> newProfile);
}
