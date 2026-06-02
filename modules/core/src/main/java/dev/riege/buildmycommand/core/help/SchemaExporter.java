package dev.riege.buildmycommand.core.help;


import dev.riege.buildmycommand.core.parse.*;
import dev.riege.buildmycommand.core.registry.*;
import java.util.ArrayList;
import java.util.List;

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
        for (RegistryArgumentSpec argument : command.arguments()) {
            CommandFormatting.appendLine(builder, "  argument " + argument.name() + ":"
                + CommandFormatting.typeName(argument.type()) + " " + CommandFormatting.schemaArgumentKind(argument.kind()));
        }
        for (RegistryOptionSpec option : command.options()) {
            String alias = option.aliasOptional()
                .map(value -> " alias=" + value)
                .orElse("");
            CommandFormatting.appendLine(builder, "  option " + option.name() + ":"
                + CommandFormatting.typeName(option.type()) + " " + CommandFormatting.schemaOptionKind(option.kind()) + alias);
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
