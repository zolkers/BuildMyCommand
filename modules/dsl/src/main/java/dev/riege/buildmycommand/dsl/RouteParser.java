package dev.riege.buildmycommand.dsl;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RouteParser {
    private RouteParser() {
    }

    public static RoutePattern parse(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("route pattern must not be blank");
        }

        String[] tokens = pattern.trim().split("\\s+");
        LiteralToken rootLiteral = null;
        List<RouteStep> steps = new ArrayList<>();
        boolean seenOption = false;

        for (String token : tokens) {
            RouteStep step = parseStep(token);
            if (step == null) {
                if (seenOption) {
                    throw new IllegalArgumentException("route options must appear after literals: " + token);
                }
                LiteralToken literal = parseLiteralToken(token);
                if (rootLiteral == null) {
                    rootLiteral = literal;
                } else {
                    steps.add(new LiteralRouteStep(literal.value(), literal.aliases()));
                }
                continue;
            }

            if (rootLiteral == null) {
                throw new IllegalArgumentException("route pattern must start with a literal");
            }
            if (step instanceof OptionRouteStep) {
                seenOption = true;
            }
            steps.add(step);
        }

        if (rootLiteral == null) {
            throw new IllegalArgumentException("route pattern must start with a literal");
        }
        return new RoutePattern(rootLiteral.value(), rootLiteral.aliases(), steps);
    }

    static String validateLiteral(String literal, String label) {
        Objects.requireNonNull(literal, label);
        if (literal.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return literal;
    }

    static String validateName(String name, String label) {
        Objects.requireNonNull(name, label);
        if (name.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '-' && character != '_') {
                throw new IllegalArgumentException("invalid " + label + ": " + name);
            }
        }
        return name;
    }

    private static LiteralToken parseLiteralToken(String token) {
        String[] parts = token.split("\\|", -1);
        if (parts.length == 0) {
            throw new IllegalArgumentException("invalid route literal: " + token);
        }

        String literal = validateLiteral(parts[0], "route literal");
        List<String> aliases = new ArrayList<>();
        for (int index = 1; index < parts.length; index++) {
            aliases.add(validateLiteral(parts[index], "route literal alias"));
        }
        return new LiteralToken(literal, aliases);
    }

    private static RouteStep parseStep(String token) {
        if (token.startsWith("<") || token.endsWith(">")) {
            if (!token.startsWith("<") || !token.endsWith(">")) {
                throw new IllegalArgumentException("invalid required argument token: " + token);
            }
            ArgumentToken argument = parseArgumentToken(token.substring(1, token.length() - 1), token);
            return new ArgumentRouteStep(argument.name(), argument.type(),
                argument.greedy() ? RouteArgumentKind.GREEDY : RouteArgumentKind.REQUIRED);
        }

        if (token.startsWith("[") || token.endsWith("]")) {
            if (!token.startsWith("[") || !token.endsWith("]")) {
                throw new IllegalArgumentException("invalid optional token: " + token);
            }
            String body = token.substring(1, token.length() - 1);
            if (body.startsWith("--")) {
                return parseOptionToken(body, token);
            }

            ArgumentToken argument = parseArgumentToken(body, token);
            return new ArgumentRouteStep(argument.name(), argument.type(),
                argument.greedy() ? RouteArgumentKind.OPTIONAL_GREEDY : RouteArgumentKind.OPTIONAL);
        }

        return null;
    }

    private static ArgumentToken parseArgumentToken(String body, String token) {
        int separator = body.indexOf(':');
        if (separator <= 0 || separator == body.length() - 1 || body.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("invalid argument token: " + token);
        }

        String name = body.substring(0, separator);
        String typeName = body.substring(separator + 1);
        boolean greedy = typeName.endsWith("...");
        if (greedy) {
            typeName = typeName.substring(0, typeName.length() - 3);
        }

        RouteType type = typeFor(typeName);
        if (greedy && type.runtimeType() != String.class) {
            throw new IllegalArgumentException("greedy route arguments must be String: " + token);
        }
        return new ArgumentToken(validateName(name, "argument name"), type, greedy);
    }

    private static OptionRouteStep parseOptionToken(String body, String token) {
        String[] parts = body.split("\\|", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("invalid option token: " + token);
        }

        String option = parts[0];
        String alias = parts.length == 2 ? parseAlias(parts[1], token) : null;
        int separator = option.indexOf(':');
        if (separator < 0) {
            String name = parseLongOptionName(option, token);
            return new OptionRouteStep(name, RouteType.runtime("Boolean", Boolean.class), alias, RouteOptionKind.FLAG);
        }
        if (separator == option.length() - 1 || option.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("invalid option token: " + token);
        }

        String name = parseLongOptionName(option.substring(0, separator), token);
        RouteType type = typeFor(option.substring(separator + 1));
        return new OptionRouteStep(name, type, alias, RouteOptionKind.VALUE);
    }

    private static String parseLongOptionName(String raw, String token) {
        if (!raw.startsWith("--") || raw.length() <= 2) {
            throw new IllegalArgumentException("invalid long option token: " + token);
        }
        return validateName(raw.substring(2), "option name");
    }

    private static String parseAlias(String raw, String token) {
        if (!raw.startsWith("-") || raw.startsWith("--") || raw.length() != 2) {
            throw new IllegalArgumentException("invalid option alias token: " + token);
        }
        return validateName(raw.substring(1), "option alias");
    }

    private static RouteType typeFor(String typeName) {
        if (typeName.startsWith("enum(") && typeName.endsWith(")")) {
            return RouteType.inlineEnum(parseEnumValues(typeName));
        }

        RouteRange range = null;
        String baseTypeName = typeName;
        int rangeStart = typeName.indexOf('{');
        if (rangeStart >= 0) {
            if (!typeName.endsWith("}")) {
                throw new IllegalArgumentException("invalid route type range: " + typeName);
            }
            baseTypeName = typeName.substring(0, rangeStart);
            range = parseRange(typeName.substring(rangeStart + 1, typeName.length() - 1), typeName);
        }

        Class<?> runtimeType = runtimeTypeFor(baseTypeName);
        if (range != null && !isNumeric(runtimeType)) {
            throw new IllegalArgumentException("route type range requires a numeric type: " + typeName);
        }
        if (range != null) {
            return RouteType.constrained(baseTypeName, runtimeType, range);
        }
        return RouteType.runtime(baseTypeName, runtimeType);
    }

    private static List<String> parseEnumValues(String typeName) {
        String body = typeName.substring("enum(".length(), typeName.length() - 1);
        if (body.isBlank()) {
            throw new IllegalArgumentException("inline enum route type must declare at least one value");
        }
        String[] values = body.split(",", -1);
        List<String> enumValues = new ArrayList<>();
        for (String value : values) {
            enumValues.add(validateName(value, "enum value"));
        }
        return enumValues;
    }

    private static RouteRange parseRange(String body, String typeName) {
        int separator = body.indexOf("..");
        if (separator < 0 || body.indexOf("..", separator + 2) >= 0) {
            throw new IllegalArgumentException("invalid route type range: " + typeName);
        }
        String minText = body.substring(0, separator);
        String maxText = body.substring(separator + 2);
        Long min = minText.isBlank() ? null : Long.valueOf(minText);
        Long max = maxText.isBlank() ? null : Long.valueOf(maxText);
        return new RouteRange(min, max);
    }

    private static Class<?> runtimeTypeFor(String typeName) {
        Class<?> type = switch (typeName) {
            case "String" -> String.class;
            case "Integer" -> Integer.class;
            case "int" -> int.class;
            case "Long" -> Long.class;
            case "long" -> long.class;
            case "Float" -> Float.class;
            case "float" -> float.class;
            case "Double" -> Double.class;
            case "double" -> double.class;
            case "Boolean" -> Boolean.class;
            case "boolean" -> boolean.class;
            case "UUID" -> UUID.class;
            case "Duration" -> Duration.class;
            case "LocalDate" -> LocalDate.class;
            case "LocalDateTime" -> LocalDateTime.class;
            case "Path" -> Path.class;
            case "URI" -> URI.class;
            case "URL" -> URL.class;
            default -> null;
        };
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("unknown route type: " + typeName);
    }

    private static boolean isNumeric(Class<?> type) {
        return type == Integer.class || type == int.class
            || type == Long.class || type == long.class
            || type == Float.class || type == float.class
            || type == Double.class || type == double.class;
    }

    private record LiteralToken(String value, List<String> aliases) {
        private LiteralToken {
            aliases = List.copyOf(aliases);
        }
    }

    private record ArgumentToken(String name, RouteType type, boolean greedy) {
    }
}
