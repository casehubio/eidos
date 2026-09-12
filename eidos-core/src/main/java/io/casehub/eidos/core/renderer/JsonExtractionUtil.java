package io.casehub.eidos.core.renderer;

class JsonExtractionUtil {

    private JsonExtractionUtil() {}

    static String extractJson(final String text) {
        if (text == null) return "";
        String s = text.strip();
        if (s.startsWith("```")) {
            final int nl = s.indexOf('\n');
            if (nl != -1) s = s.substring(nl + 1).strip();
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3).stripTrailing();
        }
        final int first = s.indexOf('{');
        final int last = s.lastIndexOf('}');
        if (first != -1 && last > first) s = s.substring(first, last + 1);
        return s;
    }
}
