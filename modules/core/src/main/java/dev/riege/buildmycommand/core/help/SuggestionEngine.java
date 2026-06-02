package dev.riege.buildmycommand.core.help;


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

    public SuggestionEngine(SimpleCommandRegistry registry, CommandTokenizer tokenizer) {
        this(registry, tokenizer, CommandMatchingPolicy.strict());
    }

    public SuggestionEngine(SimpleCommandRegistry registry, CommandTokenizer tokenizer, CommandMatchingPolicy matchingPolicy) {
        this.registry = registry;
        this.tokenizer = tokenizer;
        this.matchingPolicy = matchingPolicy;
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

        return suggestValues(source, prefixInput, current).stream()
            .map(value -> new Suggestion(value, Optional.empty(), replacementStart, replacementEnd, typeFor(value, current), 0))
            .toList();
    }

    private List<String> suggestValues(CommandSource source, String prefixInput, String current) {
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
                .toList();
        }

        RegistryCommandNode command = registry.find(tokens.get(0));
        if (command == null) {
            return List.of();
        }

        int tokenIndex = 1;
        List<RegistryCommandNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(command);
        while (tokenIndex < tokens.size()) {
            RegistryCommandNode child = command.children().get(matchingPolicy.literalKey(tokens.get(tokenIndex)));
            if (child == null) {
                break;
            }
            command = child;
            matchedNodes.add(command);
            tokenIndex++;
        }
        if (tokenIndex < tokens.size()) {
            matchedNodes = CommandPermissions.literalDescendantPath(
                command,
                tokens.subList(tokenIndex, tokens.size()),
                matchedNodes
            );
            command = matchedNodes.get(matchedNodes.size() - 1);
        }

        if (current.startsWith("-")) {
            if (!CommandPermissions.canAccess(source, matchedNodes)) {
                return List.of();
            }
            return command.options().stream()
                .map(option -> "--" + option.name())
                .filter(name -> optionStartsWith(name, current))
                .toList();
        }
        List<RegistryCommandNode> suggestionPath = matchedNodes;
        return command.children().values().stream()
            .distinct()
            .filter(child -> CommandPermissions.canDiscover(source, CommandPermissions.append(suggestionPath, child), child))
            .map(RegistryCommandNode::literal)
            .filter(literal -> startsWith(literal, current))
            .toList();
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
}
