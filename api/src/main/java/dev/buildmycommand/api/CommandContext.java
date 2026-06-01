package dev.buildmycommand.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record CommandContext(CommandSource source, String input, Map<String, Object> arguments) {
    public CommandContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public CommandContext(CommandSource source, String input) {
        this(source, input, Map.of());
    }

    public <T> T arg(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        Object value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("argument not found: " + name);
        }
        return type.cast(value);
    }

    public <T> Optional<T> optionalArg(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        Object value = arguments.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    @SuppressWarnings("unchecked")
    public <T> T argOr(String name, T fallback) {
        Objects.requireNonNull(name, "name");

        Object value = arguments.get(name);
        if (value == null) {
            return fallback;
        }
        return (T) value;
    }
}
