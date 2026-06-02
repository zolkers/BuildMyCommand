package dev.riege.buildmycommand.core.help;


import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.registry.*;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SuggestionEngine {
    private final SimpleCommandRegistry registry;
    private final CommandTokenizer tokenizer;
    private final CommandMatchingPolicy matchingPolicy;
    private final ArgumentParserRegistry parsers;

    public SuggestionEngine(SimpleCommandRegistry registry, CommandTokenizer tokenizer) {
        this(registry, tokenizer, CommandMatchingPolicy.strict(), new ArgumentParserRegistry());
    }

    public SuggestionEngine(SimpleCommandRegistry registry, CommandTokenizer tokenizer, CommandMatchingPolicy matchingPolicy) {
        this(registry, tokenizer, matchingPolicy, new ArgumentParserRegistry());
    }

    public SuggestionEngine(
        SimpleCommandRegistry registry,
        CommandTokenizer tokenizer,
        CommandMatchingPolicy matchingPolicy,
        ArgumentParserRegistry parsers
    ) {
        this.registry = registry;
        this.tokenizer = tokenizer;
        this.matchingPolicy = matchingPolicy;
        this.parsers = parsers;
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        return suggestRich(new CommandInput(source, input, cursor, "", dev.riege.buildmycommand.api.CommandPlatform.test()))
            .stream()
            .map(Suggestion::value)
            .toList();
    }

    public List<Suggestion> suggestRich(CommandInput input) {
        CommandSource source = input.source();
        String normalizedInput = input.normalizedInput();
        int normalizedCursor = input.normalizedCursor();
        String prefixInput = normalizedInput.substring(0, Math.max(0, Math.min(normalizedCursor, normalizedInput.length())));
        int replacementEnd = normalizedCursor;
        int replacementStart = replacementStart(prefixInput);
        String current = prefixInput.substring(replacementStart);

        return suggestValues(input, prefixInput, current, replacementStart, replacementEnd);
    }

    private List<Suggestion> suggestValues(
        CommandInput input,
        String prefixInput,
        String current,
        int replacementStart,
        int replacementEnd
    ) {
        CommandSource source = input.source();
        TokenizeResult tokenizeResult = tokenizer.tokenize(prefixInput);
        if (tokenizeResult.failure().isPresent()) {
            return List.of();
        }

        List<String> tokens = tokenizeResult.tokens();
        if (tokens.isEmpty() || (tokens.size() == 1 && !prefixInput.endsWith(" "))) {
            return registry.roots().stream()
                .filter(command -> CommandPermissions.canDiscover(source, List.of(command), command))
                .map(RegistryCommandNode::literal)
                .filter(literal -> startsWith(literal, current))
                .map(value -> new Suggestion(value, Optional.empty(), replacementStart, replacementEnd,
                    typeFor(value, current), 0))
                .toList();
        }

        RegistryCommandNode command = registry.find(tokens.get(0));
        if (command == null) {
            return List.of();
        }

        List<RegistryCommandNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(command);

        int completeTokenCount = tokens.size();
        if (!prefixInput.endsWith(" ") && completeTokenCount > 0) {
            completeTokenCount--;
        }

        CompletionState state = completionState(input, tokens, completeTokenCount, command, matchedNodes);
        if (state.failureState()) {
            return List.of();
        }
        command = state.command();
        matchedNodes = state.matchedNodes();

        RegistryOptionSpec valueOption = valueOptionBeforeCurrent(command.options(), tokens, state.tokenIndex(), completeTokenCount);
        if (valueOption != null) {
            return parsers.suggestions(valueOption.type(), context(input, valueOption, current, replacementStart, replacementEnd));
        }

        if (current.startsWith("-")) {
            if (!CommandPermissions.canAccess(source, matchedNodes)) {
                return List.of();
            }
            return command.options().stream()
                .map(option -> "--" + option.name())
                .filter(name -> optionStartsWith(name, current))
                .map(value -> new Suggestion(value, Optional.empty(), replacementStart, replacementEnd,
                    typeFor(value, current), 0))
                .toList();
        }

        RegistryArgumentSpec nextArgument = nextArgument(command, tokens, state.tokenIndex(), completeTokenCount);
        if (nextArgument != null) {
            List<Suggestion> suggestions = parsers.suggestions(
                nextArgument.type(),
                context(input, nextArgument, current, replacementStart, replacementEnd)
            );
            if (!suggestions.isEmpty()) {
                return suggestions;
            }
        }

        List<RegistryCommandNode> suggestionPath = matchedNodes;
        return command.children().values().stream()
            .distinct()
            .filter(child -> CommandPermissions.canDiscover(source, CommandPermissions.append(suggestionPath, child), child))
            .map(RegistryCommandNode::literal)
            .filter(literal -> startsWith(literal, current))
            .map(value -> new Suggestion(value, Optional.empty(), replacementStart, replacementEnd,
                typeFor(value, current), 0))
            .toList();
    }

    private CompletionState completionState(
        CommandInput input,
        List<String> tokens,
        int completeTokenCount,
        RegistryCommandNode root,
        List<RegistryCommandNode> initialPath
    ) {
        RegistryCommandNode command = root;
        List<RegistryCommandNode> matchedNodes = new ArrayList<>(initialPath);
        int tokenIndex = 1;

        while (tokenIndex < completeTokenCount) {
            String token = tokens.get(tokenIndex);
            RegistryCommandNode child = command.children().get(matchingPolicy.literalKey(token));
            if (child != null) {
                command = child;
                matchedNodes.add(command);
                tokenIndex++;
                continue;
            }
            if (!command.children().isEmpty() && !command.arguments().isEmpty() && !token.startsWith("-")) {
                ArgumentResolver resolver = new ArgumentResolver(parsers);
                ParseArgumentPrefixResult prefix = resolver.parseArgumentPrefix(
                    command.arguments(),
                    tokens.subList(tokenIndex, completeTokenCount),
                    input
                );
                if (prefix.failure().isPresent()) {
                    return CompletionState.failure();
                }
                if (prefix.consumed() > 0) {
                    tokenIndex += prefix.consumed();
                    continue;
                }
            }
            break;
        }

        return CompletionState.success(command, tokenIndex, matchedNodes);
    }

    private static RegistryArgumentSpec nextArgument(
        RegistryCommandNode command,
        List<String> tokens,
        int tokenIndex,
        int completeTokenCount
    ) {
        int positionals = 0;
        int index = tokenIndex;
        while (index < completeTokenCount) {
            String token = tokens.get(index);
            RegistryOptionSpec option = findOption(command.options(), token);
            if (option == null) {
                if (isOptionLike(token)) {
                    return null;
                }
                positionals++;
                index++;
                continue;
            }
            index += option.kind() == RegistryOptionKind.VALUE ? 2 : 1;
        }
        if (positionals >= command.arguments().size()) {
            return null;
        }
        return command.arguments().get(positionals);
    }

    private static RegistryOptionSpec valueOptionBeforeCurrent(
        List<RegistryOptionSpec> options,
        List<String> tokens,
        int tokenIndex,
        int completeTokenCount
    ) {
        if (completeTokenCount <= tokenIndex) {
            return null;
        }
        RegistryOptionSpec previous = findOption(options, tokens.get(completeTokenCount - 1));
        if (previous != null && previous.kind() == RegistryOptionKind.VALUE) {
            return previous;
        }
        return null;
    }

    private static RegistryOptionSpec findOption(List<RegistryOptionSpec> options, String token) {
        for (RegistryOptionSpec option : options) {
            if (token.equals("--" + option.name())
                || option.aliasOptional().map(alias -> token.equals("-" + alias)).orElse(false)) {
                return option;
            }
        }
        return null;
    }

    private static boolean isOptionLike(String token) {
        if (token.startsWith("--")) {
            return token.length() > 2;
        }
        return token.length() > 1 && token.charAt(0) == '-' && !Character.isDigit(token.charAt(1));
    }

    private static ArgumentParseContext context(
        CommandInput input,
        RegistryArgumentSpec argument,
        String current,
        int replacementStart,
        int replacementEnd
    ) {
        return new ArgumentParseContext(input.source(), input, argument.name(), argument.type(), current,
            replacementStart, replacementEnd, SuggestionType.ARGUMENT);
    }

    private static ArgumentParseContext context(
        CommandInput input,
        RegistryOptionSpec option,
        String current,
        int replacementStart,
        int replacementEnd
    ) {
        return new ArgumentParseContext(input.source(), input, option.name(), option.type(), current,
            replacementStart, replacementEnd, SuggestionType.OPTION_VALUE);
    }

    private boolean startsWith(String literal, String current) {
        return matchingPolicy.caseInsensitiveLiterals()
            ? literal.regionMatches(true, 0, current, 0, current.length())
            : literal.startsWith(current);
    }

    private boolean optionStartsWith(String option, String current) {
        return matchingPolicy.caseInsensitiveOptions()
            ? option.regionMatches(true, 0, current, 0, current.length())
            : option.startsWith(current);
    }

    private static int replacementStart(String input) {
        int index = input.length();
        while (index > 0 && !Character.isWhitespace(input.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private static SuggestionType typeFor(String value, String current) {
        if (value.startsWith("-")) {
            return SuggestionType.FLAG;
        }
        return current.isEmpty() ? SuggestionType.SUBCOMMAND : SuggestionType.COMMAND;
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

    private record CompletionState(
        RegistryCommandNode command,
        int tokenIndex,
        List<RegistryCommandNode> matchedNodes,
        boolean failureState
    ) {
        static CompletionState success(
            RegistryCommandNode command,
            int tokenIndex,
            List<RegistryCommandNode> matchedNodes
        ) {
            return new CompletionState(command, tokenIndex, List.copyOf(matchedNodes), false);
        }

        static CompletionState failure() {
            return new CompletionState(null, 0, List.of(), true);
        }
    }
}
