package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import java.util.Objects;

public record RegistryArgumentSpec(String name, Class<?> type, RegistryArgumentKind kind) {
    public RegistryArgumentSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
    }
}
