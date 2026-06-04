package io.casehub.eidos.runtime.graph;

import io.casehub.eidos.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockingToReactiveGraphBridgeTest {

    @Mock AgentGraphQuery mockBlocking;

    @Test
    void historyByCapability_delegates_to_blocking() {
        var expected = new AgentTaskHistory("agent-x", "tenant-z",
            List.of(), List.of(), List.of(),
            GraphDataSufficiency.forCount(0, null, null, List.of()));
        when(mockBlocking.historyByCapability("agent-x", "cap-y", "tenant-z"))
            .thenReturn(expected);
        var bridge = new BlockingToReactiveGraphBridge(mockBlocking);

        var result = bridge.historyByCapability("agent-x", "cap-y", "tenant-z")
                           .await().indefinitely();

        assertThat(result).isSameAs(expected);
        verify(mockBlocking).historyByCapability("agent-x", "cap-y", "tenant-z");
    }

    @Test
    void attestationsFor_delegates_to_blocking() {
        var expected = List.of(
            new AttestationRef(null, "agent-a", "t1", "hash-xyz", "ML", Instant.now()));
        when(mockBlocking.attestationsFor("agent-a", "t1")).thenReturn(expected);
        var bridge = new BlockingToReactiveGraphBridge(mockBlocking);

        var result = bridge.attestationsFor("agent-a", "t1")
                           .await().indefinitely();

        assertThat(result).isSameAs(expected);
        verify(mockBlocking).attestationsFor("agent-a", "t1");
    }
}
