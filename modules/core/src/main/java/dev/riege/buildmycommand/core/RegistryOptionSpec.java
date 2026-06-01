package dev.riege.buildmycommand.core;

import java.util.Objects;
import java.util.Optional;

record RegistryOptionSpec(String name, Class<?> type, String alias, RegistryOptionKind kind) {
    RegistryOptionSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("flag or option name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        if (alias != null && alias.isBlank()) {
            throw new IllegalArgumentException("flag or option alias must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
    }

    Optional<String> aliasOptional() {
        return Optional.ofNullable(alias);
    }
}
