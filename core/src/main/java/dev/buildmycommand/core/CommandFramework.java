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
import java.util.function.Function;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;
    private final Map<Class<?>, Function<String, ParseResult<?>>> parsers;

    private CommandFramework(SimpleCommandRegistry registry) {
        this.registry = registry;
        this.parsers = Map.of(
            String.class, value -> ParseResult.success(value),
            int.class, value -> parseInteger(value),
            Integer.class, value -> parseInteger(value)
        );
    }

    private static ParseResult<Integer> parseInteger(String value) {
        try {
            return ParseResult.success(Integer.valueOf(value));
        } catch (NumberFormatException exception) {
            return ParseResult.failure("Invalid integer");
        }
    }

    public static CommandFramework create() {
        return new CommandFramework(new SimpleCommandRegistry());
    }

    public CommandRegistry registry() {
        return registry;
    }

    public String help(String path) {
        Objects.requireNonNull(path, "path");

        TokenizeResult tokenizeResult = tokenize(path);
        if (tokenizeResult.failure().isPresent() || tokenizeResult.tokens().isEmpty()) {
            return "Unknown command: " + path;
        }

        SimpleCommandRegistry.CommandPath commandPath = registry.findPath(tokenizeResult.tokens());
        if (commandPath == null) {
            return "Unknown command: " + path;
        }

        return "Usage: " + usage(commandPath);
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
        while (tokenIndex < tokens.size()) {
            SimpleCommandRegistry.CommandNode child = command.children().get(tokens.get(tokenIndex));
            if (child == null) {
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
            tokenIndex++;
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
                .map(SimpleCommandRegistry.CommandNode::literal)
                .filter(literal -> literal.startsWith(current))
                .toList();
        }

        SimpleCommandRegistry.CommandNode command = registry.find(tokens.get(0));
        if (command == null) {
            return List.of();
        }

        int tokenIndex = 1;
        while (tokenIndex < tokens.size()) {
            SimpleCommandRegistry.CommandNode child = command.children().get(tokens.get(tokenIndex));
            if (child == null) {
                break;
            }
            command = child;
            tokenIndex++;
        }

        if (current.startsWith("-")) {
            return command.options().stream()
                .map(option -> "--" + option.name())
                .filter(name -> name.startsWith(current))
                .toList();
        }
        return command.children().values().stream()
            .distinct()
            .map(SimpleCommandRegistry.CommandNode::literal)
            .filter(literal -> literal.startsWith(current))
            .toList();
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
        if (parser == null) {
            return ParseResult.failure("No parser registered");
        }
        return parser.apply(raw);
    }

    private TokenizeResult tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean tokenStarted = false;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character == '"') {
                quoted = !quoted;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(character) && !quoted) {
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

        if (quoted) {
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
