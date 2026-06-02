package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import java.util.Objects;
import java.util.Optional;

public record RegistryOptionSpec(String name, Class<?> type, String alias, RegistryOptionKind kind) {
    public RegistryOptionSpec {
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

    public Optional<String> aliasOptional() {
        return Optional.ofNullable(alias);
    }
}
