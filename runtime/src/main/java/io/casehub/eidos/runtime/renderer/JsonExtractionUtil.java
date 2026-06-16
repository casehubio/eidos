package io.casehub.eidos.runtime.renderer;

class JsonExtractionUtil {

    private JsonExtractionUtil() {}

    /**
     * Extracts a JSON object from LLM output that may contain code fences or prose preamble.
     * Strips markdown code fences (```json...``` or ```...```).
     * Finds the outermost {...} block, stripping both leading prose and trailing prose.
     * Returns empty string for null or no-JSON input.
     */
    static String extractJson(final String text) {
        if (text == null) return "";
        String s = text.strip();
        // Strip markdown code fences
        if (s.startsWith("```")) {
            final int nl = s.indexOf('\n');
            if (nl != -1) s = s.substring(nl + 1).strip();
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3).stripTrailing();
        }
        // Extract from preamble or strip trailing prose (finds outermost {...})
        final int first = s.indexOf('{');
        final int last = s.lastIndexOf('}');
        if (first != -1 && last > first) s = s.substring(first, last + 1);
        return s;
    }
}
