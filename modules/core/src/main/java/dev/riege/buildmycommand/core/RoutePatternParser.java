package dev.riege.buildmycommand.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class RoutePatternParser {
    private RoutePatternParser() {
    }

    static RoutePattern parse(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("route pattern must not be blank");
        }

        String[] tokens = pattern.trim().split("\\s+");
        String rootLiteral = null;
        List<RouteStep> steps = new ArrayList<>();
        boolean seenOption = false;

        for (String token : tokens) {
            RouteElement element = parseElement(token);
            if (element == null) {
                if (seenOption) {
                    throw new IllegalArgumentException("route options must appear after literals: " + token);
                }
                String literal = Validators.literal(token, "route literal");
                if (rootLiteral == null) {
                    rootLiteral = literal;
                } else {
                    steps.add(new LiteralRouteStep(literal));
                }
                continue;
            }

            if (rootLiteral == null) {
                throw new IllegalArgumentException("route pattern must start with a literal");
            }
            if (element instanceof OptionRouteElement) {
                seenOption = true;
            }
            steps.add(new ElementRouteStep(element));
        }

        if (rootLiteral == null) {
            throw new IllegalArgumentException("route pattern must start with a literal");
        }
        return new RoutePattern(rootLiteral, steps);
    }

    private static RouteElement parseElement(String token) {
        if (token.startsWith("<") || token.endsWith(">")) {
            if (!token.startsWith("<") || !token.endsWith(">")) {
                throw new IllegalArgumentException("invalid required argument token: " + token);
            }
            ArgumentToken argument = parseArgumentToken(token.substring(1, token.length() - 1), token);
            if (argument.greedy()) {
                return builder -> builder.greedyArgument(argument.name(), argument.type());
            }
            return builder -> builder.argument(argument.name(), argument.type());
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
            if (argument.greedy()) {
                return builder -> ((SimpleCommandBuilder) builder).optionalGreedyArgument(argument.name(), argument.type());
            }
            return builder -> builder.optionalArgument(argument.name(), argument.type());
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

        Class<?> type = typeFor(typeName);
        if (greedy && type != String.class) {
            throw new IllegalArgumentException("greedy route arguments must be String: " + token);
        }
        return new ArgumentToken(Validators.name(name, "argument name"), type, greedy);
    }

    private static RouteElement parseOptionToken(String body, String token) {
        String[] parts = body.split("\\|", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("invalid option token: " + token);
        }

        String option = parts[0];
        String alias = parts.length == 2 ? parseAlias(parts[1], token) : null;
        int separator = option.indexOf(':');
        if (separator < 0) {
            String name = parseLongOptionName(option, token);
            return new OptionRouteElement(builder -> builder.flag(name, alias));
        }
        if (separator == option.length() - 1 || option.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("invalid option token: " + token);
        }

        String name = parseLongOptionName(option.substring(0, separator), token);
        Class<?> type = typeFor(option.substring(separator + 1));
        return new OptionRouteElement(builder -> builder.option(name, type, alias));
    }

    private static String parseLongOptionName(String raw, String token) {
        if (!raw.startsWith("--") || raw.length() <= 2) {
            throw new IllegalArgumentException("invalid long option token: " + token);
        }
        return Validators.name(raw.substring(2), "option name");
    }

    private static String parseAlias(String raw, String token) {
        if (!raw.startsWith("-") || raw.startsWith("--") || raw.length() != 2) {
            throw new IllegalArgumentException("invalid option alias token: " + token);
        }
        return Validators.name(raw.substring(1), "option alias");
    }

    private static Class<?> typeFor(String typeName) {
        Class<?> type = switch (typeName) {
            case "String" -> String.class;
            case "Integer" -> Integer.class;
            case "int" -> int.class;
            case "Long" -> Long.class;
            case "long" -> long.class;
            case "Double" -> Double.class;
            case "double" -> double.class;
            case "Boolean" -> Boolean.class;
            case "boolean" -> boolean.class;
            case "UUID" -> UUID.class;
            default -> null;
        };
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("unknown route type: " + typeName);
    }
}
