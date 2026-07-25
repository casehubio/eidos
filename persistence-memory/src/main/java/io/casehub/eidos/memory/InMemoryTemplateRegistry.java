package io.casehub.eidos.memory;

import io.casehub.eidos.api.DescriptorTemplate;
import io.casehub.eidos.api.TemplateRegistry;
import io.casehub.eidos.api.spi.TemplateRegistrar;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryTemplateRegistry implements TemplateRegistry {

    private static final Pattern                                       PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private final        ConcurrentHashMap<String, DescriptorTemplate> store       = new ConcurrentHashMap<>();

    @Inject
    @Any
    Instance<TemplateRegistrar> registrars;

    static void validatePlaceholders(DescriptorTemplate template) {
        var matcher    = PLACEHOLDER.matcher(template.content());
        var declared   = Set.copyOf(template.parameters());
        var undeclared = new ArrayList<String>();
        while (matcher.find()) {
            var param = matcher.group(1);
            if (!declared.contains(param)) {undeclared.add(param);}
        }
        if (!undeclared.isEmpty()) {
            throw new IllegalStateException("Template '" + template.id()
                                            + "' has undeclared placeholder(s): " + undeclared);
        }
    }

    @PostConstruct
    void init() {
        if (registrars != null) {
            for (var r : registrars) {
                r.templates().forEach(this::register);
            }
        }
    }

    @Override
    public void register(DescriptorTemplate template) {
        validatePlaceholders(template);
        if (store.putIfAbsent(template.id(), template) != null) {
            throw new IllegalStateException("Duplicate template ID: " + template.id());
        }
    }

    @Override
    public Optional<DescriptorTemplate> resolve(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<DescriptorTemplate> all() {
        return List.copyOf(store.values());
    }

    public void clear() {
        store.clear();
    }
}
