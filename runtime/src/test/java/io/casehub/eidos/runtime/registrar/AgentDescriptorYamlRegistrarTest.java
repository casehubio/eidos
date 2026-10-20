package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.VocabularyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AgentDescriptorYamlRegistrarTest {

    @Mock AgentRegistry registry;
    @Mock VocabularyRegistry vocabRegistry;

    AgentDescriptorYamlRegistrar registrar;

    @BeforeEach
    void setUp() {
        registrar = new AgentDescriptorYamlRegistrar(registry, vocabRegistry);
    }

    @Test
    void sectionKey_isAgents() {
        assertThat(registrar.sectionKey()).isEqualTo("agents");
    }

    @Test
    void classpathPaths_scansAgentsDirectory() {
        assertThat(registrar.classpathPaths()).isEqualTo(Set.of("agents"));
    }

    @Test
    void priority_is100() {
        assertThat(registrar.priority()).isEqualTo(100);
    }

    @Test
    void register_parsesYamlAndRegistersDescriptor() {
        String yaml = """
                descriptors:
                  - agentId: test-agent
                    name: Test Agent
                    slot: tester
                    tenancyId: default
                """;
        var stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        registrar.register("agents/test-agent.yaml", stream);

        var captor = ArgumentCaptor.forClass(AgentDescriptor.class);
        verify(registry).register(captor.capture());
        assertThat(captor.getValue().agentId()).isEqualTo("test-agent");
        assertThat(captor.getValue().name()).isEqualTo("Test Agent");
        assertThat(captor.getValue().slot()).isEqualTo("tester");
    }

    @Test
    void register_multipleDescriptorsInOneFile() {
        String yaml = """
                descriptors:
                  - agentId: agent-1
                    name: Agent One
                    slot: planner
                    tenancyId: default
                  - agentId: agent-2
                    name: Agent Two
                    slot: reviewer
                    tenancyId: default
                """;
        var stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        registrar.register("agents/team.yaml", stream);

        verify(registry, org.mockito.Mockito.times(2)).register(
            org.mockito.ArgumentMatchers.any(AgentDescriptor.class));
    }

    @Test
    void register_emptyYaml_skips() {
        var stream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        registrar.register("agents/empty.yaml", stream);

        verifyNoInteractions(registry);
    }
}
