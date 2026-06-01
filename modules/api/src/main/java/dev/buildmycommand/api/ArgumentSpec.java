package dev.buildmycommand.api;

import java.util.Objects;

public record ArgumentSpec<T>(String name, Class<T> type, Kind kind) {
    public ArgumentSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
    }

    public enum Kind {
        REQUIRED,
        OPTIONAL,
        GREEDY,
        OPTIONAL_GREEDY
    }
}
