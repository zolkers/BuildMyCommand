package dev.riege.buildmycommand.core.help;


import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.registry.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SchemaExporter {
    public String schema(SimpleCommandRegistry registry) {
        StringBuilder builder = new StringBuilder();
        for (RegistryCommandNode root : registry.roots()) {
            appendSchema(builder, List.of(root.literal()), root);
        }
        return builder.toString();
    }

    private static void appendSchema(
        StringBuilder builder,
        List<String> path,
        RegistryCommandNode command
    ) {
        CommandFormatting.appendLine(builder, "command " + String.join(" ", path));
        command.descriptionOptional()
            .ifPresent(description -> CommandFormatting.appendLine(builder, "  description " + description));
        command.permissionOptional()
            .ifPresent(permission -> CommandFormatting.appendLine(builder, "  permission " + permission));
        if (command.metadata().hidden()) {
            CommandFormatting.appendLine(builder, "  hidden true");
        }
        command.metadata().group()
            .ifPresent(group -> CommandFormatting.appendLine(builder, "  group " + group));
        command.metadata().usage()
            .ifPresent(usage -> CommandFormatting.appendLine(builder, "  usage " + usage));
        command.metadata().examples()
            .forEach(example -> CommandFormatting.appendLine(builder, "  example " + example));
        command.metadata().cooldown()
            .ifPresent(cooldown -> CommandFormatting.appendLine(builder, "  cooldown " + cooldown));
        command.metadata().requirement()
            .ifPresent(requirement -> CommandFormatting.appendLine(builder, "  require " + requirement));
        for (RegistryArgumentSpec argument : command.arguments()) {
            String suggestions = argument.suggestionProviderOptional()
                .flatMap(ignored -> Optional.ofNullable(argument.suggestionProviderName()))
                .map(name -> " suggest=" + name)
                .orElse("");
            CommandFormatting.appendLine(builder, "  argument " + argument.name() + ":"
                + CommandFormatting.typeName(argument.type()) + " " + CommandFormatting.schemaArgumentKind(argument.kind())
                + suggestions);
        }
        for (RegistryOptionSpec option : command.options()) {
            String alias = option.aliasOptional()
                .map(value -> " alias=" + value)
                .orElse("");
            String suggestions = option.suggestionProviderOptional()
                .flatMap(ignored -> Optional.ofNullable(option.suggestionProviderName()))
                .map(name -> " suggest=" + name)
                .orElse("");
            CommandFormatting.appendLine(builder, "  option " + option.name() + ":"
                + CommandFormatting.typeName(option.type()) + " " + CommandFormatting.schemaOptionKind(option.kind()) + alias
                + suggestions);
        }
        for (RegistryCommandNode child : command.uniqueChildren()) {
            CommandFormatting.appendLine(builder, "  child " + child.literal());
        }
        for (RegistryCommandNode child : command.uniqueChildren()) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(child.literal());
            appendSchema(builder, childPath, child);
        }
    }
}
