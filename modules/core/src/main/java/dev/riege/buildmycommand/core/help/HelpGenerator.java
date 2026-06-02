package dev.riege.buildmycommand.core.help;


import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.registry.*;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HelpGenerator {
    private final SimpleCommandRegistry registry;
    private final CommandTokenizer tokenizer;

    public HelpGenerator(SimpleCommandRegistry registry, CommandTokenizer tokenizer) {
        this.registry = registry;
        this.tokenizer = tokenizer;
    }

    public String help(CommandSource source, String path) {
        TokenizeResult tokenizeResult = tokenizer.tokenize(path);
        if (tokenizeResult.failure().isPresent() || tokenizeResult.tokens().isEmpty()) {
            return "Unknown command: " + path;
        }

        RegistryCommandPath commandPath = registry.findPath(tokenizeResult.tokens());
        if (commandPath == null) {
            return "Unknown command: " + path;
        }
        if (commandPath.nodes().stream().anyMatch(node -> node.metadata().hidden())) {
            return "Unknown command: " + path;
        }

        Optional<String> deniedPermission = CommandPermissions.deniedPermission(source, commandPath.nodes());
        if (deniedPermission.isPresent()) {
            return "Missing permission: " + deniedPermission.get();
        }

        StringBuilder builder = new StringBuilder("Usage: ")
            .append(commandPath.node().metadata().usage().orElseGet(() -> usage(commandPath)));
        commandPath.node().descriptionOptional()
            .ifPresent(description -> CommandFormatting.appendLine(builder, "Description: " + description));
        commandPath.node().metadata().examples()
            .forEach(example -> CommandFormatting.appendLine(builder, "Example: " + example));
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
