package io.casehub.eidos.org.runtime.trust;

import java.util.Map;

public final class OrgDistanceUtil {

    public static final String SNAPSHOT_KEY = "org-distance";

    public static int extractDistance(Map<String, Object> snapshot) {
        if (snapshot == null || !snapshot.containsKey(SNAPSHOT_KEY)) {
            return -1;
        }
        Object raw = snapshot.get(SNAPSHOT_KEY);
        if (!(raw instanceof Number)) {
            return -1;
        }
        return ((Number) raw).intValue();
    }

    private OrgDistanceUtil() {}
}
