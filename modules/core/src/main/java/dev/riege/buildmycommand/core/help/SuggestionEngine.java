package dev.riege.buildmycommand.core.help;


import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.registry.*;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.List;

public final class SuggestionEngine {
    private final SimpleCommandRegistry registry;
    private final CommandTokenizer tokenizer;

    public SuggestionEngine(SimpleCommandRegistry registry, CommandTokenizer tokenizer) {
        this.registry = registry;
        this.tokenizer = tokenizer;
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        String prefixInput = input.substring(0, Math.max(0, Math.min(cursor, input.length())));
        TokenizeResult tokenizeResult = tokenizer.tokenize(prefixInput);
        if (tokenizeResult.failure().isPresent()) {
            return List.of();
        }

        List<String> tokens = tokenizeResult.tokens();
        String current = currentToken(prefixInput, tokens);
        if (tokens.isEmpty() || (tokens.size() == 1 && !prefixInput.endsWith(" "))) {
            return registry.roots().stream()
                .filter(command -> CommandPermissions.canDiscover(source, List.of(command), command))
                .map(RegistryCommandNode::literal)
                .filter(literal -> literal.startsWith(current))
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
            RegistryCommandNode child = command.children().get(tokens.get(tokenIndex));
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
                .filter(name -> name.startsWith(current))
                .toList();
        }
        List<RegistryCommandNode> suggestionPath = matchedNodes;
        return command.children().values().stream()
            .distinct()
            .filter(child -> CommandPermissions.canDiscover(source, CommandPermissions.append(suggestionPath, child), child))
            .map(RegistryCommandNode::literal)
            .filter(literal -> literal.startsWith(current))
            .toList();
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
