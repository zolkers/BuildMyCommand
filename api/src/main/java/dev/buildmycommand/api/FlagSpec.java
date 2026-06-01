package dev.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public record FlagSpec<T>(String name, Class<T> type, String alias, Kind kind) {
    public FlagSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("flag or option name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
        if (alias != null && alias.isBlank()) {
            throw new IllegalArgumentException("flag or option alias must not be blank");
        }
    }

    public Optional<String> aliasOptional() {
        return Optional.ofNullable(alias);
    }

    public FlagSpec<T> alias(String alias) {
        return new FlagSpec<>(name, type, alias, kind);
    }

    public enum Kind {
        FLAG,
        VALUE
    }
}
