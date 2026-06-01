package io.casehub.eidos.api;

import java.util.Set;

class AgentDescriptorValidator {

    // Required field bounds
    private static final int MAX_AGENT_ID   = 255;
    private static final int MAX_NAME       = 200;
    private static final int MAX_SLOT       = 100;
    private static final int MAX_TENANCY_ID = 255;

    // Optional field bounds — accessible within package for compact constructors
    static final int MAX_VERSION             = 200;
    static final int MAX_PROVIDER            = 200;
    static final int MAX_WEIGHTS_FINGERPRINT = 255;
    static final int MAX_VOCABULARY_URI      = 500;
    static final int MAX_JURISDICTION        = 1000;
    static final int MAX_DISPOSITION_AXIS    = 200;
    static final int MAX_CAPABILITY_NAME     = 100;
    static final int MAX_CAPABILITY_STRING   = 200;

    static void validate(final String agentId, final String name,
                          final String slot, final String tenancyId) {
        validateField("agentId",   agentId,   MAX_AGENT_ID);
        validateField("name",      name,      MAX_NAME);
        validateField("slot",      slot,      MAX_SLOT);
        validateField("tenancyId", tenancyId, MAX_TENANCY_ID);
    }

    // Unlike validateOptional, passes null to validateField — where it throws.
    static void validateRequired(final String fieldName, final String value, final int maxLength) {
        validateField(fieldName, value, maxLength);
    }

    static void validateOptional(final String fieldName, final String value, final int maxLength) {
        if (value == null) return;
        validateField(fieldName, value, maxLength);
    }

    static void validateItems(final String fieldName, final Iterable<String> items,
                               final int maxLength) {
        if (items == null) return;
        int index = 0;
        for (final String item : items) {
            validateOptional(fieldName + "[" + index + "]", item, maxLength);
            index++;
        }
    }

    static void validateMapKeys(final String fieldName, final Set<String> keys,
                                 final int maxLength) {
        if (keys == null) return;
        for (final String key : keys) {
            validateOptional(fieldName + ".key", key, maxLength);
        }
    }

    private static void validateField(final String fieldName, final String value,
                                       final int maxLength) {
        if (value == null) {
            throw new AgentValidationException(fieldName, "must not be null");
        }
        if (value.isBlank()) {
            throw new AgentValidationException(fieldName, "must not be blank");
        }
        if (value.length() > maxLength) {
            throw new AgentValidationException(fieldName,
                "exceeds maximum length " + maxLength + " (was " + value.length() + ")");
        }
        for (int i = 0; i < value.length(); ) {
            final int cp = value.codePointAt(i);
            if (isBanned(cp)) {
                throw new AgentValidationException(fieldName,
                    "contains banned character U+" + String.format("%04X", cp));
            }
            i += Character.charCount(cp);
        }
    }

    private static boolean isBanned(final int cp) {
        if (cp <= 0x001F) return true;                      // C0 control chars (0-31)
        if (cp >= 0x007F && cp <= 0x009F) return true;     // DEL + C1 control chars (127-159)
        if (cp == 0x061C) return true;                      // ALM — Arabic Letter Mark (BiDi control)
        if (cp == 0x200E || cp == 0x200F) return true;     // LRM, RLM
        if (cp >= 0x202A && cp <= 0x202E) return true;     // LRE, RLE, PDF, LRO, RLO
        if (cp >= 0x2066 && cp <= 0x2069) return true;     // LRI, RLI, FSI, PDI
        if (cp == 0x200B || cp == 0xFEFF) return true;     // ZWSP, BOM/ZWNBSP
        if (cp == 0x200C || cp == 0x200D) return true;     // ZWNJ, ZWJ
        if (cp == 0x2028 || cp == 0x2029) return true;     // LINE SEP, PARA SEP
        return false;
    }
}
