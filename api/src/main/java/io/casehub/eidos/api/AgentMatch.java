package io.casehub.eidos.api;

/**
 * Result of {@link AgentRegistry#find(AgentQuery)} — an agent that matched the query,
 * with optional capability resolution context.
 *
 * <p>{@code resolvedCapability} is non-null when {@link AgentQuery#capabilityName()} was specified,
 * carrying the declared capability that matched and the OWLS-MX match degree.
 * Null for slot-only or {@link AgentQuery#all} queries where no capability matching occurred.
 *
 * @param descriptor the matched agent descriptor
 * @param resolvedCapability the capability resolution result, or null when no capability was queried
 */
public record AgentMatch(AgentDescriptor descriptor, ResolvedCapability resolvedCapability) {}
