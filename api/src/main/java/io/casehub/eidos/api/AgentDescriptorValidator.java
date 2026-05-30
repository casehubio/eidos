package io.casehub.eidos.api;

class AgentDescriptorValidator {

    // Bounds chosen to cap cache key length and LLM payload size.
    private static final int MAX_AGENT_ID   = 255;
    private static final int MAX_NAME       = 200;
    private static final int MAX_SLOT       = 100;
    private static final int MAX_TENANCY_ID = 255;

    static void validate(final String agentId, final String name,
                          final String slot, final String tenancyId) {
        validateField("agentId",   agentId,   MAX_AGENT_ID);
        validateField("name",      name,      MAX_NAME);
        validateField("slot",      slot,      MAX_SLOT);
        validateField("tenancyId", tenancyId, MAX_TENANCY_ID);
    }

    private static void validateField(final String fieldName, final String value,
                                       final int maxLength) {
        if (value == null) {
            throw new AgentDescriptorValidationException(fieldName, "must not be null");
        }
        if (value.isBlank()) {
            throw new AgentDescriptorValidationException(fieldName, "must not be blank");
        }
        if (value.length() > maxLength) {
            throw new AgentDescriptorValidationException(fieldName,
                "exceeds maximum length " + maxLength + " (was " + value.length() + ")");
        }
        for (int i = 0; i < value.length(); ) {
            final int cp = value.codePointAt(i);
            if (isBanned(cp)) {
                throw new AgentDescriptorValidationException(fieldName,
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
