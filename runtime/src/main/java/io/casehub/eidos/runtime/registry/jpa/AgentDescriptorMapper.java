package io.casehub.eidos.runtime.registry.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.TemplateRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
class AgentDescriptorMapper {

    @Inject
    ObjectMapper mapper;

    AgentDescriptor toRecord(AgentDescriptorEntity e) {
        return new AgentDescriptor(
                e.agentId, e.name, e.version, e.provider,
                e.modelFamily, e.modelVersion, e.weightsFingerprint,
                e.domainVocabulary, e.slotVocabulary, e.dispositionVocabulary,
                readJson(e.axisVocabularies, new TypeReference<Map<DispositionAxis, String>>() {}),
                e.slot,
                e.capabilities.stream().map(this::toCapability).toList(),
                readJson(e.disposition, AgentDisposition.class),
                e.jurisdiction, e.dataHandlingPolicy, e.tenancyId,
                e.briefing,
                readJson(e.templates, new TypeReference<List<TemplateRef>>() {})
        );
    }

    AgentDescriptorEntity toEntity(AgentDescriptor d) {
        var e = new AgentDescriptorEntity();
        e.agentId               = d.agentId();
        e.tenancyId             = d.tenancyId();
        e.name                  = d.name();
        e.version               = d.version();
        e.provider              = d.provider();
        e.modelFamily           = d.modelFamily();
        e.modelVersion          = d.modelVersion();
        e.weightsFingerprint    = d.weightsFingerprint();
        e.domainVocabulary      = d.domainVocabulary();
        e.slotVocabulary        = d.slotVocabulary();
        e.dispositionVocabulary = d.dispositionVocabulary();
        e.axisVocabularies      = writeJson(d.axisVocabularies());
        e.slot                  = d.slot();
        e.jurisdiction          = d.jurisdiction();
        e.dataHandlingPolicy    = d.dataHandlingPolicy();
        e.briefing              = d.briefing();
        e.templates             = writeJson(d.templates());
        e.disposition           = writeJson(d.disposition());
        d.capabilities().stream()
         .map(c -> toCapabilityEntity(c, e))
         .forEach(e.capabilities::add);
        return e;
    }

    private AgentCapability toCapability(AgentCapabilityEntity c) {
        return new AgentCapability(
                c.name,
                c.description,
                c.capabilityVocabulary,
                c.qualityHint,
                c.latencyHintP50Ms,
                c.costHint,
                readJson(c.inputTypes, new TypeReference<List<String>>() {}),
                readJson(c.outputTypes, new TypeReference<List<String>>() {}),
                readJson(c.tags, new TypeReference<List<String>>() {}),
                readJson(c.epistemicDomains, new TypeReference<Map<String, Double>>() {}),
                c.excludedDomains
        );
    }

    private AgentCapabilityEntity toCapabilityEntity(AgentCapability c, AgentDescriptorEntity parent) {
        var e = new AgentCapabilityEntity();
        e.descriptor           = parent;
        e.agentId              = parent.agentId;
        e.tenancyId            = parent.tenancyId;
        e.name                 = c.name();
        e.description          = c.description();
        e.capabilityVocabulary = c.capabilityVocabulary();
        e.qualityHint          = c.qualityHint();
        e.latencyHintP50Ms     = c.latencyHintP50Ms();
        e.costHint             = c.costHint();
        e.inputTypes           = writeJson(c.inputTypes());
        e.outputTypes          = writeJson(c.outputTypes());
        e.tags                 = writeJson(c.tags());
        e.epistemicDomains     = writeJson(c.epistemicDomains());
        e.excludedDomains      = c.excludedDomains();
        return e;
    }

    private <T> T readJson(String json, Class<T> type) {
        if (json == null) {return null;}
        try {return mapper.readValue(json, type);} catch (Exception ex) {
            throw new RuntimeException("JSON deserialisation failed", ex);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        if (json == null) {return null;}
        try {return mapper.readValue(json, type);} catch (Exception ex) {
            throw new RuntimeException("JSON deserialisation failed", ex);
        }
    }

    private String writeJson(Object obj) {
        if (obj == null) {return null;}
        try {return mapper.writeValueAsString(obj);} catch (Exception ex) {
            throw new RuntimeException("JSON serialisation failed", ex);
        }
    }
}
