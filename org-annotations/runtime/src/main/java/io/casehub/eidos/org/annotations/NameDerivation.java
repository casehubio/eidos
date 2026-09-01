package io.casehub.eidos.org.annotations;

public final class NameDerivation {

    private NameDerivation() {}

    public static String toKebabCase(String className) {
        if (className == null || className.isEmpty()) return "";
        int dollar = className.lastIndexOf('$');
        if (dollar >= 0) className = className.substring(dollar + 1);
        var sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean nextIsLower = i + 1 < className.length() && Character.isLowerCase(className.charAt(i + 1));
                boolean prevIsUpper = i > 0 && Character.isUpperCase(className.charAt(i - 1));
                if (i > 0 && (!prevIsUpper || nextIsLower)) sb.append('-');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toDisplayName(String className) {
        if (className == null || className.isEmpty()) return "";
        int dollar = className.lastIndexOf('$');
        if (dollar >= 0) className = className.substring(dollar + 1);
        var sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                boolean prevIsUpper = Character.isUpperCase(className.charAt(i - 1));
                boolean nextIsLower = i + 1 < className.length() && Character.isLowerCase(className.charAt(i + 1));
                if (!prevIsUpper || nextIsLower) sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
