package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.FlagSpec;

import java.util.List;
import java.util.Optional;

public final class DiscordSlashCommandSync {
    public List<DiscordSlashCommand> commands(CommandGraph graph) {
        return graph.roots().stream()
            .filter(node -> !node.metadata().hidden())
            .map(this::command)
            .toList();
    }

    private DiscordSlashCommand command(CommandNode node) {
        return new DiscordSlashCommand(
            node.literal(),
            node.description().orElse("No description provided."),
            node.aliases(),
            options(node),
            node.children().stream()
                .filter(child -> !child.metadata().hidden())
                .map(this::command)
                .toList(),
            node.permission()
        );
    }

    private static List<DiscordSlashOption> options(CommandNode node) {
        List<DiscordSlashOption> arguments = node.arguments().stream()
            .map(DiscordSlashCommandSync::argument)
            .toList();
        List<DiscordSlashOption> flags = node.flags().stream()
            .map(DiscordSlashCommandSync::flag)
            .toList();
        return java.util.stream.Stream.concat(arguments.stream(), flags.stream()).toList();
    }

    private static DiscordSlashOption argument(ArgumentSpec<?> argument) {
        return new DiscordSlashOption(
            argument.name(),
            type(argument.type()),
            argument.kind() == ArgumentSpec.Kind.REQUIRED || argument.kind() == ArgumentSpec.Kind.GREEDY,
            "Argument " + argument.name()
        );
    }

    private static DiscordSlashOption flag(FlagSpec<?> flag) {
        return new DiscordSlashOption(
            flag.name(),
            type(flag.type()),
            false,
            Optional.ofNullable(flag.alias()).map(alias -> "Alias -" + alias).orElse("Option " + flag.name())
        );
    }

    private static DiscordSlashOptionType type(Class<?> type) {
        if (type == Boolean.class || type == boolean.class) {
            return DiscordSlashOptionType.BOOLEAN;
        }
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
            return DiscordSlashOptionType.INTEGER;
        }
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            return DiscordSlashOptionType.NUMBER;
        }
        return DiscordSlashOptionType.STRING;
    }
}
