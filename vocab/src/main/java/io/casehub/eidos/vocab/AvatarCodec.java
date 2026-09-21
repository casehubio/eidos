package io.casehub.eidos.vocab;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AvatarCodec {

    public static final String DEFAULT_COLLECTION = "mythic";

    private static final List<String> CANONICAL_ORDER;

    static {
        CANONICAL_ORDER = Arrays.stream(ArchetypeTerm.values())
            .sorted(Comparator.comparing((ArchetypeTerm t) -> t.family().name())
                               .thenComparing(ArchetypeTerm::value))
            .map(ArchetypeTerm::value)
            .toList();
    }

    private AvatarCodec() {}

    public static int archetypeIndex(String archetypeValue) {
        return CANONICAL_ORDER.indexOf(archetypeValue.toLowerCase(Locale.ROOT));
    }

    public static String defaultCode(String collection, String archetypeValue) {
        int idx = archetypeIndex(archetypeValue);
        if (idx < 0) return null;
        return collection + ":P" + Integer.toString(idx, 36).toUpperCase();
    }

    public static boolean isExternalUrl(String avatar) {
        if (avatar == null) return false;
        String lower = avatar.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
