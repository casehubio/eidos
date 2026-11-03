package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.GroupInstanceQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedGroupInstanceStoreTest {

    private final UnsupportedGroupInstanceStore store = new UnsupportedGroupInstanceStore();

    @Test void saveThrows() {
        assertThatThrownBy(() -> store.save(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("No GroupInstanceStore configured");
    }

    @Test void findByIdThrows() {
        assertThatThrownBy(() -> store.findById("id", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void findThrows() {
        assertThatThrownBy(() -> store.find(new GroupInstanceQuery("t1", null, null, null)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void removeThrows() {
        assertThatThrownBy(() -> store.remove("id", "t1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
