package dev.riege.buildmycommand.core;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

final class ArgumentParserRegistry {
    private final Map<Class<?>, Function<String, ParseResult<?>>> parsers = Map.of(
        String.class, value -> ParseResult.success(value),
        int.class, ArgumentParserRegistry::parseInteger,
        Integer.class, ArgumentParserRegistry::parseInteger,
        long.class, ArgumentParserRegistry::parseLong,
        Long.class, ArgumentParserRegistry::parseLong,
        double.class, ArgumentParserRegistry::parseDouble,
        Double.class, ArgumentParserRegistry::parseDouble,
        boolean.class, ArgumentParserRegistry::parseBoolean,
        Boolean.class, ArgumentParserRegistry::parseBoolean,
        UUID.class, ArgumentParserRegistry::parseUuid
    );

    ParseResult<?> parse(Class<?> type, String raw) {
        Function<String, ParseResult<?>> parser = parsers.get(type);
        if (parser != null) {
            return parser.apply(raw);
        }
        if (type.isEnum()) {
            return parseEnum(type, raw);
        }
        return ParseResult.failure("No parser registered");
    }

    private static ParseResult<Integer> parseInteger(String value) {
        try {
            return ParseResult.success(Integer.valueOf(value));
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid integer");
        }
    }

    private static ParseResult<Long> parseLong(String value) {
        try {
            return ParseResult.success(Long.valueOf(value));
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid long");
        }
    }

    private static ParseResult<Double> parseDouble(String value) {
        try {
            Double parsed = Double.valueOf(value);
            if (!Double.isFinite(parsed)) {
                return ParseResult.failure("Invalid double");
            }
            return ParseResult.success(parsed);
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid double");
        }
    }

    private static ParseResult<Boolean> parseBoolean(String value) {
        if ("true".equals(value)) {
            return ParseResult.success(true);
        }
        if ("false".equals(value)) {
            return ParseResult.success(false);
        }
        return ParseResult.failure("Invalid boolean");
    }

    private static ParseResult<UUID> parseUuid(String value) {
        try {
            return ParseResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return ParseResult.failure("Invalid UUID");
        }
    }

    private static ParseResult<?> parseEnum(Class<?> type, String raw) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(raw)) {
                return ParseResult.success(constant);
            }
        }
        return ParseResult.failure("Invalid enum");
    }
}
