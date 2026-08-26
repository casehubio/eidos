package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectionContextTest {

    @Test
    void nullTenancyIdRejected() {
        assertThrows(NullPointerException.class,
            () -> new SelectionContext(null, "cap", null));
    }

    @Test
    void twoArgFactoryMatchesRecordOrder() {
        var ctx = SelectionContext.of("tenant-1", "code-review");
        assertEquals("tenant-1", ctx.tenancyId());
        assertEquals("code-review", ctx.capabilityName());
        assertNull(ctx.taskDomain());
    }

    @Test
    void threeArgFactoryMatchesRecordOrder() {
        var ctx = SelectionContext.of("tenant-1", "code-review", "java");
        assertEquals("tenant-1", ctx.tenancyId());
        assertEquals("code-review", ctx.capabilityName());
        assertEquals("java", ctx.taskDomain());
    }
}
