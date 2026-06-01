package dev.riege.buildmycommand.core;

import java.util.Objects;

record RegistryArgumentSpec(String name, Class<?> type, RegistryArgumentKind kind) {
    RegistryArgumentSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
    }
}
