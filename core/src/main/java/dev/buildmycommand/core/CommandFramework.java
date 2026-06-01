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
        while (tokenIndex < tokens.size()) {
            SimpleCommandRegistry.CommandNode child = command.children().get(tokens.get(tokenIndex));
            if (child == null) {
                break;
            }
            command = child;
            tokenIndex++;
        }

        ParseArgumentsResult arguments = parseArguments(command.arguments(), tokens.subList(tokenIndex, tokens.size()));
        if (arguments.failure().isPresent()) {
            return Results.failure(arguments.failure().get());
        }

        CommandContext context = new CommandContext(source, input, arguments.values());
        return Objects.requireNonNull(command.executor().execute(context), "command result");
    }

    private ParseArgumentsResult parseArguments(
        List<SimpleCommandRegistry.ArgumentSpec> specs,
        List<String> tokens
    ) {
        Map<String, Object> values = new HashMap<>();
        int tokenIndex = 0;

        for (SimpleCommandRegistry.ArgumentSpec spec : specs) {
            if (spec.kind() == SimpleCommandRegistry.ArgumentKind.GREEDY) {
                if (tokenIndex >= tokens.size()) {
                    return ParseArgumentsResult.failure("Missing required argument: " + spec.name());
                }
                String raw = String.join(" ", tokens.subList(tokenIndex, tokens.size()));
                ParseResult<?> parsed = parse(spec, raw);
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
            ParseResult<?> parsed = parse(spec, raw);
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

    private ParseResult<?> parse(SimpleCommandRegistry.ArgumentSpec spec, String raw) {
        Function<String, ParseResult<?>> parser = parsers.get(spec.type());
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

    private record ParseArgumentsResult(Map<String, Object> values, Optional<String> failure) {
        static ParseArgumentsResult success(Map<String, Object> values) {
            return new ParseArgumentsResult(Map.copyOf(values), Optional.empty());
        }

        static ParseArgumentsResult failure(String failure) {
            return new ParseArgumentsResult(Map.of(), Optional.of(failure));
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
