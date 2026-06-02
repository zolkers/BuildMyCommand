package dev.riege.buildmycommand.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record CommandContext(
    CommandSource source,
    String input,
    CommandInput commandInput,
    Map<String, Object> arguments
) {
    public CommandContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(commandInput, "commandInput");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public CommandContext(CommandSource source, String input) {
        this(source, input, Map.of());
    }

    public CommandContext(CommandSource source, String input, Map<String, Object> arguments) {
        this(source, input, CommandInput.raw(source, input), arguments);
    }

    public CommandContext(CommandSource source, CommandInput commandInput, Map<String, Object> arguments) {
        this(source, commandInput.normalizedInput(), commandInput, arguments);
    }

    public <T> T arg(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        Object value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("argument not found: " + name);
        }
        return cast(type, value);
    }

    public <T> Optional<T> optionalArg(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        Object value = arguments.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(cast(type, value));
    }

    public boolean flag(String name) {
        Objects.requireNonNull(name, "name");
        Object value = arguments.get(name);
        if (value == null) {
            return false;
        }
        return cast(Boolean.class, value);
    }

    public <T> Optional<T> option(String name, Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        Object value = arguments.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(cast(type, value));
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

    @SuppressWarnings("unchecked")
    private static <T> T cast(Class<T> type, Object value) {
        Class<?> effectiveType = type.isPrimitive() ? primitiveWrapper(type) : type;
        Object castValue = effectiveType.cast(value);
        return (T) castValue;
    }

    private static Class<?> primitiveWrapper(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
