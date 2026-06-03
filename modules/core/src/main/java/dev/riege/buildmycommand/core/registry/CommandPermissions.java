package dev.riege.buildmycommand.core.registry;

import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.requirement.RequirementExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CommandPermissions {
    private CommandPermissions() {
    }

    public static List<RegistryCommandNode> literalDescendantPath(
        RegistryCommandNode command,
        List<String> tokens,
        List<RegistryCommandNode> currentPath
    ) {
        List<RegistryCommandNode> path = new ArrayList<>(currentPath);
        RegistryCommandNode current = command;
        for (String token : tokens) {
            RegistryCommandNode child = current.children().get(token);
            if (child == null) {
                continue;
            }
            path.add(child);
            current = child;
        }
        return path;
    }

    public static List<RegistryCommandNode> append(
        List<RegistryCommandNode> nodes,
        RegistryCommandNode child
    ) {
        List<RegistryCommandNode> appended = new ArrayList<>(nodes);
        appended.add(child);
        return appended;
    }

    public static boolean canAccess(CommandSource source, List<RegistryCommandNode> commands) {
        return deniedPermission(source, commands).isEmpty();
    }

    public static boolean canDiscover(
        CommandSource source,
        List<RegistryCommandNode> path,
        RegistryCommandNode command
    ) {
        if (command.metadata().hidden()) {
            return false;
        }
        if (!canAccess(source, path)) {
            return false;
        }
        if (command.isExecutable()) {
            return true;
        }
        for (RegistryCommandNode child : command.uniqueChildren()) {
            if (canDiscover(source, append(path, child), child)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<String> deniedPermission(
        CommandSource source,
        List<RegistryCommandNode> commands
    ) {
        for (RegistryCommandNode command : commands) {
            Optional<String> permission = command.permissionOptional();
            if (permission.isPresent() && !source.hasPermission(permission.get())) {
                return permission;
            }
            Optional<String> requirement = command.metadata().requirement();
            if (requirement.isPresent() && !matchesRequirement(source, requirement.get())) {
                return requirement;
            }
        }
        return Optional.empty();
    }

    private static boolean matchesRequirement(CommandSource source, String expression) {
        return RequirementExpression.evaluate(expression, source::hasPermission);
    }
}
