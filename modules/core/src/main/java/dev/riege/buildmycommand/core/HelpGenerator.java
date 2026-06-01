package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class HelpGenerator {
    private final SimpleCommandRegistry registry;
    private final CommandTokenizer tokenizer;

    HelpGenerator(SimpleCommandRegistry registry, CommandTokenizer tokenizer) {
        this.registry = registry;
        this.tokenizer = tokenizer;
    }

    String help(CommandSource source, String path) {
        TokenizeResult tokenizeResult = tokenizer.tokenize(path);
        if (tokenizeResult.failure().isPresent() || tokenizeResult.tokens().isEmpty()) {
            return "Unknown command: " + path;
        }

        RegistryCommandPath commandPath = registry.findPath(tokenizeResult.tokens());
        if (commandPath == null) {
            return "Unknown command: " + path;
        }

        Optional<String> deniedPermission = CommandPermissions.deniedPermission(source, commandPath.nodes());
        if (deniedPermission.isPresent()) {
            return "Missing permission: " + deniedPermission.get();
        }

        StringBuilder builder = new StringBuilder("Usage: ").append(usage(commandPath));
        commandPath.node().descriptionOptional()
            .ifPresent(description -> CommandFormatting.appendLine(builder, "Description: " + description));
        return builder.toString();
    }

    private static String usage(RegistryCommandPath commandPath) {
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < commandPath.nodes().size(); index++) {
            parts.add(commandPath.literals().get(index));
            parts.addAll(commandPath.nodes().get(index).arguments().stream()
                .map(CommandFormatting::usageArgument)
                .toList());
        }
        parts.addAll(commandPath.node().options().stream()
            .map(CommandFormatting::usageOption)
            .toList());
        return String.join(" ", parts);
    }
}
