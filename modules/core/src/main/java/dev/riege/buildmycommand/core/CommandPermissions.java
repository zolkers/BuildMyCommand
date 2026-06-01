package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CommandPermissions {
    private CommandPermissions() {
    }

    static List<RegistryCommandNode> literalDescendantPath(
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

    static List<RegistryCommandNode> append(
        List<RegistryCommandNode> nodes,
        RegistryCommandNode child
    ) {
        List<RegistryCommandNode> appended = new ArrayList<>(nodes);
        appended.add(child);
        return appended;
    }

    static boolean canAccess(CommandSource source, List<RegistryCommandNode> commands) {
        return deniedPermission(source, commands).isEmpty();
    }

    static boolean canDiscover(
        CommandSource source,
        List<RegistryCommandNode> path,
        RegistryCommandNode command
    ) {
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

    static Optional<String> deniedPermission(
        CommandSource source,
        List<RegistryCommandNode> commands
    ) {
        for (RegistryCommandNode command : commands) {
            Optional<String> permission = command.permissionOptional();
            if (permission.isPresent() && !source.hasPermission(permission.get())) {
                return permission;
            }
        }
        return Optional.empty();
    }
}
