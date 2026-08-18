package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import static org.assertj.core.api.Assertions.*;

class DiscoverableAnnotationTest {

    @Test
    void retentionIsRuntime() {
        assertThat(Discoverable.class.getAnnotation(java.lang.annotation.Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void targetIsType() {
        assertThat(Discoverable.class.getAnnotation(java.lang.annotation.Target.class).value())
            .containsExactly(ElementType.TYPE);
    }

    @Test
    void capabilitiesIsRequired() throws NoSuchMethodException {
        var method = Discoverable.class.getDeclaredMethod("capabilities");
        assertThat(method.getDefaultValue()).isNull();
    }
}
