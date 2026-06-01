package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandContext;
import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;
    private final Map<Class<?>, Function<String, ParseResult<?>>> parsers;

    private CommandFramework(SimpleCommandRegistry registry) {
        this.registry = registry;
        this.parsers = Map.of(
            String.class, value -> ParseResult.success(value),
            int.class, value -> parseInteger(value),
            Integer.class, value -> parseInteger(value),
            long.class, value -> parseLong(value),
            Long.class, value -> parseLong(value),
            double.class, value -> parseDouble(value),
            Double.class, value -> parseDouble(value),
            boolean.class, value -> parseBoolean(value),
            Boolean.class, value -> parseBoolean(value),
            UUID.class, value -> parseUuid(value)
        );
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
            return ParseResult.success(Double.valueOf(value));
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

    public static CommandFramework create() {
        return new CommandFramework(new SimpleCommandRegistry());
    }

    public CommandRegistry registry() {
        return registry;
    }

    public String help(String path) {
        return help(new CommandSource() {
        }, path);
    }

    public String help(CommandSource source, String path) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(path, "path");

        TokenizeResult tokenizeResult = tokenize(path);
        if (tokenizeResult.failure().isPresent() || tokenizeResult.tokens().isEmpty()) {
            return "Unknown command: " + path;
        }

        SimpleCommandRegistry.CommandPath commandPath = registry.findPath(tokenizeResult.tokens());
        if (commandPath == null) {
            return "Unknown command: " + path;
        }

        Optional<String> deniedPermission = deniedPermission(source, commandPath.nodes());
        if (deniedPermission.isPresent()) {
            return "Missing permission: " + deniedPermission.get();
        }

        StringBuilder builder = new StringBuilder("Usage: ").append(usage(commandPath));
        commandPath.node().descriptionOptional()
            .ifPresent(description -> appendLine(builder, "Description: " + description));
        return builder.toString();
    }

    public String schema() {
        StringBuilder builder = new StringBuilder();
        for (SimpleCommandRegistry.CommandNode root : registry.roots()) {
            appendSchema(builder, List.of(root.literal()), root);
        }
        return builder.toString();
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");

        TokenizeResult tokenizeResult = tokenize(input);
        if (tokenizeResult.failure().isPresent()) {
            return Results.failure(tokenizeResult.failure().get());
        }

        List<String> tokens = tokenizeResult.tokens();
        if (tokens.isEmpty()) {
            return Results.failure("Unknown command: " + input);
        }

        String literal = tokens.get(0);
        SimpleCommandRegistry.CommandNode command = registry.find(literal);
        if (command == null) {
            return Results.failure("Unknown command: " + literal);
        }

        int tokenIndex = 1;
        Map<String, Object> pathValues = new HashMap<>();
        List<SimpleCommandRegistry.CommandNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(command);
        while (tokenIndex < tokens.size()) {
            SimpleCommandRegistry.CommandNode child = command.children().get(tokens.get(tokenIndex));
            if (child == null) {
                List<SimpleCommandRegistry.CommandNode> possiblePath = literalDescendantPath(
                    command,
                    tokens.subList(tokenIndex, tokens.size()),
                    matchedNodes
                );
                Optional<String> deniedPermission = deniedPermission(source, possiblePath);
                if (deniedPermission.isPresent()) {
                    return Results.failure("Missing permission: " + deniedPermission.get());
                }
                if (!command.children().isEmpty() && !command.arguments().isEmpty()) {
                    ParseArgumentPrefixResult prefix = parseArgumentPrefix(command.arguments(), tokens.subList(tokenIndex, tokens.size()));
                    if (prefix.failure().isPresent()) {
                        return Results.failure(prefix.failure().get());
                    }
                    if (prefix.consumed() > 0) {
                        pathValues.putAll(prefix.values());
                        tokenIndex += prefix.consumed();
                        continue;
                    }
                }
                break;
            }
            command = child;
            matchedNodes.add(command);
            tokenIndex++;
        }

        Optional<String> deniedPermission = deniedPermission(source, matchedNodes);
        if (deniedPermission.isPresent()) {
            return Results.failure("Missing permission: " + deniedPermission.get());
        }

        ParseOptionsResult options = parseOptions(command.options(), tokens.subList(tokenIndex, tokens.size()));
        if (options.failure().isPresent()) {
            return Results.failure(options.failure().get());
        }

        ParseArgumentsResult arguments = parseArguments(command.arguments(), options.positionals());
        if (arguments.failure().isPresent()) {
            return Results.failure(arguments.failure().get());
        }

        Map<String, Object> values = new HashMap<>(pathValues);
        values.putAll(arguments.values());
        values.putAll(options.values());
        CommandContext context = new CommandContext(source, input, values);
        return Objects.requireNonNull(command.executor().execute(context), "command result");
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");

        String prefixInput = input.substring(0, Math.max(0, Math.min(cursor, input.length())));
        TokenizeResult tokenizeResult = tokenize(prefixInput);
        if (tokenizeResult.failure().isPresent()) {
            return List.of();
        }

        List<String> tokens = tokenizeResult.tokens();
        String current = currentToken(prefixInput, tokens);
        if (tokens.isEmpty() || (tokens.size() == 1 && !prefixInput.endsWith(" "))) {
            return registry.roots().stream()
                .filter(command -> canDiscover(source, List.of(command), command))
                .map(SimpleCommandRegistry.CommandNode::literal)
                .filter(literal -> literal.startsWith(current))
                .toList();
        }

        SimpleCommandRegistry.CommandNode command = registry.find(tokens.get(0));
        if (command == null) {
            return List.of();
        }

        int tokenIndex = 1;
        List<SimpleCommandRegistry.CommandNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(command);
        while (tokenIndex < tokens.size()) {
            SimpleCommandRegistry.CommandNode child = command.children().get(tokens.get(tokenIndex));
            if (child == null) {
                break;
            }
            command = child;
            matchedNodes.add(command);
            tokenIndex++;
        }
        if (tokenIndex < tokens.size()) {
            matchedNodes = literalDescendantPath(command, tokens.subList(tokenIndex, tokens.size()), matchedNodes);
            command = matchedNodes.get(matchedNodes.size() - 1);
        }

        if (current.startsWith("-")) {
            if (!canAccess(source, matchedNodes)) {
                return List.of();
            }
            return command.options().stream()
                .map(option -> "--" + option.name())
                .filter(name -> name.startsWith(current))
                .toList();
        }
        List<SimpleCommandRegistry.CommandNode> suggestionPath = matchedNodes;
        return command.children().values().stream()
            .distinct()
            .filter(child -> canDiscover(source, append(suggestionPath, child), child))
            .map(SimpleCommandRegistry.CommandNode::literal)
            .filter(literal -> literal.startsWith(current))
            .toList();
    }

    private static List<SimpleCommandRegistry.CommandNode> literalDescendantPath(
        SimpleCommandRegistry.CommandNode command,
        List<String> tokens,
        List<SimpleCommandRegistry.CommandNode> currentPath
    ) {
        List<SimpleCommandRegistry.CommandNode> path = new ArrayList<>(currentPath);
        SimpleCommandRegistry.CommandNode current = command;
        for (String token : tokens) {
            SimpleCommandRegistry.CommandNode child = current.children().get(token);
            if (child == null) {
                continue;
            }
            path.add(child);
            current = child;
        }
        return path;
    }

    private static List<SimpleCommandRegistry.CommandNode> append(
        List<SimpleCommandRegistry.CommandNode> nodes,
        SimpleCommandRegistry.CommandNode child
    ) {
        List<SimpleCommandRegistry.CommandNode> appended = new ArrayList<>(nodes);
        appended.add(child);
        return appended;
    }

    private static boolean canAccess(CommandSource source, List<SimpleCommandRegistry.CommandNode> commands) {
        return deniedPermission(source, commands).isEmpty();
    }

    private static boolean canDiscover(
        CommandSource source,
        List<SimpleCommandRegistry.CommandNode> path,
        SimpleCommandRegistry.CommandNode command
    ) {
        if (!canAccess(source, path)) {
            return false;
        }
        if (command.isExecutable()) {
            return true;
        }
        for (SimpleCommandRegistry.CommandNode child : command.uniqueChildren()) {
            if (canDiscover(source, append(path, child), child)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> deniedPermission(
        CommandSource source,
        List<SimpleCommandRegistry.CommandNode> commands
    ) {
        for (SimpleCommandRegistry.CommandNode command : commands) {
            Optional<String> permission = command.permissionOptional();
            if (permission.isPresent() && !source.hasPermission(permission.get())) {
                return permission;
            }
        }
        return Optional.empty();
    }

    private static String usage(SimpleCommandRegistry.CommandPath commandPath) {
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < commandPath.nodes().size(); index++) {
            parts.add(commandPath.literals().get(index));
            parts.addAll(commandPath.nodes().get(index).arguments().stream()
                .map(CommandFramework::formatUsageArgument)
                .toList());
        }
        parts.addAll(commandPath.node().options().stream()
            .map(CommandFramework::formatUsageOption)
            .toList());
        return String.join(" ", parts);
    }

    private static String formatUsageArgument(SimpleCommandRegistry.ArgumentSpec argument) {
        String body = argument.name() + ":" + typeName(argument.type());
        return switch (argument.kind()) {
            case REQUIRED -> "<" + body + ">";
            case OPTIONAL -> "[" + body + "]";
            case GREEDY -> "<" + body + "...>";
            case OPTIONAL_GREEDY -> "[" + body + "...]";
        };
    }

    private static String formatUsageOption(SimpleCommandRegistry.OptionSpec option) {
        StringBuilder builder = new StringBuilder("[--").append(option.name());
        if (option.kind() == SimpleCommandRegistry.OptionKind.VALUE) {
            builder.append(":").append(typeName(option.type()));
        }
        option.aliasOptional().ifPresent(alias -> builder.append("|-").append(alias));
        return builder.append("]").toString();
    }

    private static void appendSchema(
        StringBuilder builder,
        List<String> path,
        SimpleCommandRegistry.CommandNode command
    ) {
        appendLine(builder, "command " + String.join(" ", path));
        command.descriptionOptional()
            .ifPresent(description -> appendLine(builder, "  description " + description));
        command.permissionOptional()
            .ifPresent(permission -> appendLine(builder, "  permission " + permission));
        for (SimpleCommandRegistry.ArgumentSpec argument : command.arguments()) {
            appendLine(builder, "  argument " + argument.name() + ":" + typeName(argument.type())
                + " " + schemaArgumentKind(argument.kind()));
        }
        for (SimpleCommandRegistry.OptionSpec option : command.options()) {
            String alias = option.aliasOptional()
                .map(value -> " alias=" + value)
                .orElse("");
            appendLine(builder, "  option " + option.name() + ":" + typeName(option.type())
                + " " + schemaOptionKind(option.kind()) + alias);
        }
        for (SimpleCommandRegistry.CommandNode child : command.uniqueChildren()) {
            appendLine(builder, "  child " + child.literal());
        }
        for (SimpleCommandRegistry.CommandNode child : command.uniqueChildren()) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(child.literal());
            appendSchema(builder, childPath, child);
        }
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private static String schemaArgumentKind(SimpleCommandRegistry.ArgumentKind kind) {
        return switch (kind) {
            case REQUIRED -> "required";
            case OPTIONAL -> "optional";
            case GREEDY -> "greedy";
            case OPTIONAL_GREEDY -> "optional-greedy";
        };
    }

    private static String schemaOptionKind(SimpleCommandRegistry.OptionKind kind) {
        return switch (kind) {
            case FLAG -> "flag";
            case VALUE -> "value";
        };
    }

    private static String typeName(Class<?> type) {
        return type == int.class ? "int" : type.getSimpleName();
    }

    private static String currentToken(String input, List<String> tokens) {
        if (input.isEmpty() || input.endsWith(" ")) {
            return "";
        }
        if (tokens.isEmpty()) {
            return "";
        }
        return tokens.get(tokens.size() - 1);
    }

    private ParseOptionsResult parseOptions(
        List<SimpleCommandRegistry.OptionSpec> specs,
        List<String> tokens
    ) {
        Map<String, Object> values = new HashMap<>();
        List<String> positionals = new ArrayList<>();

        int tokenIndex = 0;
        while (tokenIndex < tokens.size()) {
            String token = tokens.get(tokenIndex);
            SimpleCommandRegistry.OptionSpec spec = findOption(specs, token);
            if (spec == null) {
                if (isOptionLike(token)) {
                    return ParseOptionsResult.failure("Unknown flag or option: " + token);
                }
                positionals.add(token);
                tokenIndex++;
                continue;
            }

            if (spec.kind() == SimpleCommandRegistry.OptionKind.FLAG) {
                values.put(spec.name(), true);
                tokenIndex++;
                continue;
            }

            if (tokenIndex + 1 >= tokens.size()) {
                return ParseOptionsResult.failure("Missing value for option: " + spec.name());
            }

            String raw = tokens.get(tokenIndex + 1);
            ParseResult<?> parsed = parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseOptionsResult.failure(parsed.failure().get() + " for option " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex += 2;
        }

        return ParseOptionsResult.success(values, positionals);
    }

    private static boolean isOptionLike(String token) {
        if (token.startsWith("--")) {
            return token.length() > 2;
        }
        return token.length() > 1 && token.charAt(0) == '-' && !Character.isDigit(token.charAt(1));
    }

    private static SimpleCommandRegistry.OptionSpec findOption(
        List<SimpleCommandRegistry.OptionSpec> specs,
        String token
    ) {
        for (SimpleCommandRegistry.OptionSpec spec : specs) {
            if (token.equals("--" + spec.name()) || spec.aliasOptional().map(alias -> token.equals("-" + alias)).orElse(false)) {
                return spec;
            }
        }
        return null;
    }

    private ParseArgumentsResult parseArguments(
        List<SimpleCommandRegistry.ArgumentSpec> specs,
        List<String> tokens
    ) {
        Map<String, Object> values = new HashMap<>();
        int tokenIndex = 0;

        for (SimpleCommandRegistry.ArgumentSpec spec : specs) {
            if (spec.kind() == SimpleCommandRegistry.ArgumentKind.GREEDY
                || spec.kind() == SimpleCommandRegistry.ArgumentKind.OPTIONAL_GREEDY) {
                if (tokenIndex >= tokens.size()) {
                    if (spec.kind() == SimpleCommandRegistry.ArgumentKind.GREEDY) {
                        return ParseArgumentsResult.failure("Missing required argument: " + spec.name());
                    }
                    continue;
                }
                String raw = String.join(" ", tokens.subList(tokenIndex, tokens.size()));
                ParseResult<?> parsed = parse(spec.type(), raw);
                if (parsed.failure().isPresent()) {
                    return ParseArgumentsResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
                }
                values.put(spec.name(), parsed.value());
                tokenIndex = tokens.size();
                continue;
            }

            if (tokenIndex >= tokens.size()) {
                if (spec.kind() == SimpleCommandRegistry.ArgumentKind.REQUIRED) {
                    return ParseArgumentsResult.failure("Missing required argument: " + spec.name());
                }
                continue;
            }

            String raw = tokens.get(tokenIndex);
            ParseResult<?> parsed = parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseArgumentsResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex++;
        }

        if (tokenIndex < tokens.size()) {
            return ParseArgumentsResult.failure("Unexpected argument: " + tokens.get(tokenIndex));
        }

        return ParseArgumentsResult.success(values);
    }

    private ParseArgumentPrefixResult parseArgumentPrefix(
        List<SimpleCommandRegistry.ArgumentSpec> specs,
        List<String> tokens
    ) {
        Map<String, Object> values = new HashMap<>();
        int tokenIndex = 0;

        for (SimpleCommandRegistry.ArgumentSpec spec : specs) {
            if (spec.kind() == SimpleCommandRegistry.ArgumentKind.GREEDY
                || spec.kind() == SimpleCommandRegistry.ArgumentKind.OPTIONAL_GREEDY) {
                return ParseArgumentPrefixResult.failure("greedy arguments cannot appear before subcommands: " + spec.name());
            }

            if (tokenIndex >= tokens.size()) {
                if (spec.kind() == SimpleCommandRegistry.ArgumentKind.REQUIRED) {
                    return ParseArgumentPrefixResult.failure("Missing required argument: " + spec.name());
                }
                continue;
            }

            String raw = tokens.get(tokenIndex);
            ParseResult<?> parsed = parse(spec.type(), raw);
            if (parsed.failure().isPresent()) {
                return ParseArgumentPrefixResult.failure(parsed.failure().get() + " for argument " + spec.name() + ": " + raw);
            }
            values.put(spec.name(), parsed.value());
            tokenIndex++;
        }

        return ParseArgumentPrefixResult.success(values, tokenIndex);
    }

    private ParseResult<?> parse(Class<?> type, String raw) {
        Function<String, ParseResult<?>> parser = parsers.get(type);
        if (parser != null) {
            return parser.apply(raw);
        }
        if (type.isEnum()) {
            return parseEnum(type, raw);
        }
        return ParseResult.failure("No parser registered");
    }

    private static ParseResult<?> parseEnum(Class<?> type, String raw) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(raw)) {
                return ParseResult.success(constant);
            }
        }
        return ParseResult.failure("Invalid enum");
    }

    private TokenizeResult tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean tokenStarted = false;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (escaped) {
                current.append(character);
                tokenStarted = true;
                escaped = false;
                continue;
            }

            if (character == '\\') {
                if (index + 1 >= input.length()) {
                    current.append(character);
                    tokenStarted = true;
                    continue;
                }
                char next = input.charAt(index + 1);
                if (next != '\\' && next != '"' && next != '\'' && !Character.isWhitespace(next)) {
                    current.append(character);
                    tokenStarted = true;
                    continue;
                }
                escaped = true;
                continue;
            }

            if ((character == '"' || character == '\'') && quote == 0) {
                quote = character;
                tokenStarted = true;
                continue;
            }

            if (character == quote) {
                quote = 0;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(character) && quote == 0) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(character);
            tokenStarted = true;
        }

        if (quote != 0) {
            return TokenizeResult.failure("Unclosed quote");
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return TokenizeResult.success(tokens);
    }

    private record TokenizeResult(List<String> tokens, Optional<String> failure) {
        static TokenizeResult success(List<String> tokens) {
            return new TokenizeResult(List.copyOf(tokens), Optional.empty());
        }

        static TokenizeResult failure(String failure) {
            return new TokenizeResult(List.of(), Optional.of(failure));
        }
    }

    private record ParseOptionsResult(Map<String, Object> values, List<String> positionals, Optional<String> failure) {
        static ParseOptionsResult success(Map<String, Object> values, List<String> positionals) {
            return new ParseOptionsResult(Map.copyOf(values), List.copyOf(positionals), Optional.empty());
        }

        static ParseOptionsResult failure(String failure) {
            return new ParseOptionsResult(Map.of(), List.of(), Optional.of(failure));
        }
    }

    private record ParseArgumentsResult(Map<String, Object> values, Optional<String> failure) {
        static ParseArgumentsResult success(Map<String, Object> values) {
            return new ParseArgumentsResult(Map.copyOf(values), Optional.empty());
        }

        static ParseArgumentsResult failure(String failure) {
            return new ParseArgumentsResult(Map.of(), Optional.of(failure));
        }
    }

    private record ParseArgumentPrefixResult(Map<String, Object> values, int consumed, Optional<String> failure) {
        static ParseArgumentPrefixResult success(Map<String, Object> values, int consumed) {
            return new ParseArgumentPrefixResult(Map.copyOf(values), consumed, Optional.empty());
        }

        static ParseArgumentPrefixResult failure(String failure) {
            return new ParseArgumentPrefixResult(Map.of(), 0, Optional.of(failure));
        }
    }

    private record ParseResult<T>(T value, Optional<String> failure) {
        static <T> ParseResult<T> success(T value) {
            return new ParseResult<>(value, Optional.empty());
        }

        static <T> ParseResult<T> failure(String failure) {
            return new ParseResult<>(null, Optional.of(failure));
        }
    }
}
