package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.AgentRelationship;
import io.casehub.eidos.org.api.OrganizationalUnit;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.eidos.org.api.spi.OrgRegistrar;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrgBootstrapTest {

    @Test void registersUnitsAndRelationshipsFromSingleRegistrar() {
        var registry = new InMemoryOrgRegistry();
        var unit = OrganizationalUnit.builder().unitId("u1").name("Unit 1").tenancyId("t1").build();
        var rel = AgentRelationship.builder()
            .sourceAgentId("a1").targetAgentId("a2").kind(RelationshipKind.SUPERVISES).tenancyId("t1").build();
        var registrar = (OrgRegistrar) () -> new OrgRegistrar.OrgDefinition(List.of(unit), List.of(rel));

        var bootstrap = new OrgBootstrap();
        bootstrap.registry = registry;
        bootstrap.registrars = new SingletonInstance<>(registrar);
        bootstrap.onStartup(new StartupEvent());

        assertThat(registry.findUnit("u1", "t1")).isPresent();
        assertThat(registry.subordinates("a1", "t1")).hasSize(1);
    }

    @Test void mergesMultipleRegistrars() {
        var registry = new InMemoryOrgRegistry();
        var u1 = OrganizationalUnit.builder().unitId("u1").name("One").tenancyId("t1").build();
        var u2 = OrganizationalUnit.builder().unitId("u2").name("Two").tenancyId("t1").build();
        var r1 = (OrgRegistrar) () -> new OrgRegistrar.OrgDefinition(List.of(u1), List.of());
        var r2 = (OrgRegistrar) () -> new OrgRegistrar.OrgDefinition(List.of(u2), List.of());

        var bootstrap = new OrgBootstrap();
        bootstrap.registry = registry;
        bootstrap.registrars = new ListInstance<>(List.of(r1, r2));
        bootstrap.onStartup(new StartupEvent());

        assertThat(registry.findUnit("u1", "t1")).isPresent();
        assertThat(registry.findUnit("u2", "t1")).isPresent();
    }

    @Test void emptyRegistrarRegistersNothing() {
        var registry = new InMemoryOrgRegistry();
        var registrar = (OrgRegistrar) () -> new OrgRegistrar.OrgDefinition(List.of(), List.of());

        var bootstrap = new OrgBootstrap();
        bootstrap.registry = registry;
        bootstrap.registrars = new SingletonInstance<>(registrar);
        bootstrap.onStartup(new StartupEvent());

        assertThat(registry.findUnit("nonexistent", "t1")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static class SingletonInstance<T> implements jakarta.enterprise.inject.Instance<T> {
        private final T value;
        SingletonInstance(T value) { this.value = value; }
        @Override public T get() { return value; }
        @Override public java.util.Iterator<T> iterator() { return List.of(value).iterator(); }
        @Override public jakarta.enterprise.inject.Instance<T> select(java.lang.annotation.Annotation... qualifiers) { return this; }
        @Override public <U extends T> jakarta.enterprise.inject.Instance<U> select(Class<U> subtype, java.lang.annotation.Annotation... qualifiers) { throw new UnsupportedOperationException(); }
        @Override public <U extends T> jakarta.enterprise.inject.Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, java.lang.annotation.Annotation... qualifiers) { throw new UnsupportedOperationException(); }
        @Override public boolean isUnsatisfied() { return false; }
        @Override public boolean isAmbiguous() { return false; }
        @Override public void destroy(T instance) {}
        @Override public jakarta.enterprise.inject.Instance.Handle<T> getHandle() { throw new UnsupportedOperationException(); }
        @Override public Iterable<? extends jakarta.enterprise.inject.Instance.Handle<T>> handles() { throw new UnsupportedOperationException(); }
        @Override public boolean isResolvable() { return true; }
    }

    @SuppressWarnings("unchecked")
    private static class ListInstance<T> implements jakarta.enterprise.inject.Instance<T> {
        private final List<T> values;
        ListInstance(List<T> values) { this.values = values; }
        @Override public T get() { return values.getFirst(); }
        @Override public java.util.Iterator<T> iterator() { return values.iterator(); }
        @Override public jakarta.enterprise.inject.Instance<T> select(java.lang.annotation.Annotation... qualifiers) { return this; }
        @Override public <U extends T> jakarta.enterprise.inject.Instance<U> select(Class<U> subtype, java.lang.annotation.Annotation... qualifiers) { throw new UnsupportedOperationException(); }
        @Override public <U extends T> jakarta.enterprise.inject.Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, java.lang.annotation.Annotation... qualifiers) { throw new UnsupportedOperationException(); }
        @Override public boolean isUnsatisfied() { return values.isEmpty(); }
        @Override public boolean isAmbiguous() { return values.size() > 1; }
        @Override public void destroy(T instance) {}
        @Override public jakarta.enterprise.inject.Instance.Handle<T> getHandle() { throw new UnsupportedOperationException(); }
        @Override public Iterable<? extends jakarta.enterprise.inject.Instance.Handle<T>> handles() { throw new UnsupportedOperationException(); }
        @Override public boolean isResolvable() { return values.size() == 1; }
    }
}
