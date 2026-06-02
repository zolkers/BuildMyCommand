package dev.riege.buildmycommand.core.dispatch;


import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.middleware.MiddlewareChain;
import dev.riege.buildmycommand.core.registry.*;
import dev.riege.buildmycommand.api.CommandErrorHandler;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandDispatcher {
    private final SimpleCommandRegistry registry;
    private final CommandTokenizer tokenizer;
    private final OptionParser optionParser;
    private final ArgumentResolver argumentResolver;
    private final CommandMatchingPolicy matchingPolicy;
    private final MiddlewareChain middlewareChain;
    private final CommandErrorHandler errorHandler;

    public CommandDispatcher(
        SimpleCommandRegistry registry,
        CommandTokenizer tokenizer,
        OptionParser optionParser,
        ArgumentResolver argumentResolver,
        CommandMatchingPolicy matchingPolicy,
        MiddlewareChain middlewareChain,
        CommandErrorHandler errorHandler
    ) {
        this.registry = registry;
        this.tokenizer = tokenizer;
        this.optionParser = optionParser;
        this.argumentResolver = argumentResolver;
        this.matchingPolicy = Objects.requireNonNull(matchingPolicy, "matchingPolicy");
        this.middlewareChain = Objects.requireNonNull(middlewareChain, "middlewareChain");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return dispatch(CommandInput.raw(source, input));
    }

    public CommandResult dispatch(CommandInput input) {
        Objects.requireNonNull(input, "input");
        CommandSource source = input.source();
        String normalizedInput = input.normalizedInput();
        TokenizeResult tokenizeResult = tokenizer.tokenize(normalizedInput);
        if (tokenizeResult.failure().isPresent()) {
            return Results.failure(tokenizeResult.failure().get());
        }

        List<String> tokens = tokenizeResult.tokens();
        if (tokens.isEmpty()) {
            return Results.failure("Unknown command: " + normalizedInput);
        }

        RegistryCommandNode command = registry.find(tokens.get(0));
        if (command == null) {
            return Results.failure("Unknown command: " + tokens.get(0));
        }

        MatchResult match = matchCommandPath(input, tokens, command);
        if (match.failure().isPresent()) {
            return Results.failure(match.failure().get());
        }

        Optional<String> deniedPermission = CommandPermissions.deniedPermission(source, match.matchedNodes());
        if (deniedPermission.isPresent()) {
            return Results.failure("Missing permission: " + deniedPermission.get());
        }

        ParseOptionsResult options = optionParser.parseOptions(
            match.command().options(),
            tokens.subList(match.tokenIndex(), tokens.size()),
            input
        );
        if (options.failure().isPresent()) {
            return Results.failure(options.failure().get());
        }

        ParseArgumentsResult arguments = argumentResolver.parseArguments(match.command().arguments(), options.positionals(), input);
        if (arguments.failure().isPresent()) {
            return Results.failure(arguments.failure().get());
        }

        Map<String, Object> values = new HashMap<>(match.pathValues());
        values.putAll(arguments.values());
        values.putAll(options.values());
        CommandContext context = new CommandContext(source, input, values);
        CommandNode commandSnapshot = ManualCommandImporter.exportNode(match.command());
        List<String> commandPath = match.matchedNodes().stream()
            .map(RegistryCommandNode::literal)
            .toList();
        try {
            return middlewareChain.execute(
                context,
                commandSnapshot,
                commandPath,
                nextContext -> match.command().executor().execute(nextContext)
            );
        } catch (Throwable error) {
            return Objects.requireNonNull(
                errorHandler.handle(context, commandSnapshot, commandPath, error),
                "command result"
            );
        }
    }

    private MatchResult matchCommandPath(
        CommandInput input,
        List<String> tokens,
        RegistryCommandNode root
    ) {
        CommandSource source = input.source();
        RegistryCommandNode command = root;
        int tokenIndex = 1;
        Map<String, Object> pathValues = new HashMap<>();
        List<RegistryCommandNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(command);

        while (tokenIndex < tokens.size()) {
            RegistryCommandNode child = command.children().get(matchingPolicy.literalKey(tokens.get(tokenIndex)));
            if (child == null) {
                List<RegistryCommandNode> possiblePath = CommandPermissions.literalDescendantPath(
                    command,
                    tokens.subList(tokenIndex, tokens.size()),
                    matchedNodes
                );
                Optional<String> deniedPermission = CommandPermissions.deniedPermission(source, possiblePath);
                if (deniedPermission.isPresent()) {
                    return MatchResult.failure("Missing permission: " + deniedPermission.get());
                }
                if (!command.children().isEmpty() && !command.arguments().isEmpty()) {
                    ParseArgumentPrefixResult prefix = argumentResolver.parseArgumentPrefix(
                        command.arguments(),
                        tokens.subList(tokenIndex, tokens.size()),
                        input
                    );
                    if (prefix.failure().isPresent()) {
                        return MatchResult.failure(prefix.failure().get());
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

        return MatchResult.success(command, tokenIndex, pathValues, matchedNodes);
    }

    private record MatchResult(
        RegistryCommandNode command,
        int tokenIndex,
        Map<String, Object> pathValues,
        List<RegistryCommandNode> matchedNodes,
        Optional<String> failure
    ) {
        static MatchResult success(
            RegistryCommandNode command,
            int tokenIndex,
            Map<String, Object> pathValues,
            List<RegistryCommandNode> matchedNodes
        ) {
            return new MatchResult(command, tokenIndex, Map.copyOf(pathValues), List.copyOf(matchedNodes), Optional.empty());
        }

        static MatchResult failure(String failure) {
            return new MatchResult(null, 0, Map.of(), List.of(), Optional.of(failure));
        }
    }
}
