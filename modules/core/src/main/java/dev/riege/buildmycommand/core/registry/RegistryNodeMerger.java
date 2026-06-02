package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.CommandRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegistryNodeMerger {
    private RegistryNodeMerger() {
    }

    static void registerAll(
        Map<String, RegistryCommandNode> nodes,
        List<String> literals,
        RegistryCommandNode node,
        String duplicateMessage
    ) {
        for (String literal : literals) {
            if (nodes.containsKey(literal)) {
                throw new IllegalArgumentException(duplicateMessage + literal);
            }
        }
        for (String literal : literals) {
            nodes.put(literal, node);
        }
    }

    static void mergeRoot(Map<String, RegistryCommandNode> commands, RegistryCommandNode node) {
        RegistryCommandNode existing = commands.get(node.literal());
        if (existing == null) {
            registerAll(commands, node.literals(), node, "command already registered: ");
            return;
        }

        RegistryCommandNode merged = mergeNodes(existing, node);
        replaceNode(commands, existing, merged);
    }

    private static RegistryCommandNode mergeNodes(RegistryCommandNode existing, RegistryCommandNode incoming) {
        if (!existing.literal().equals(incoming.literal())) {
            throw new IllegalArgumentException("cannot merge different literals: " + incoming.literal());
        }

        List<RegistryArgumentSpec> arguments = mergeSpecs(existing.arguments(), incoming.arguments());
        List<RegistryOptionSpec> options = mergeSpecs(existing.options(), incoming.options());
        Map<String, RegistryCommandNode> children = new LinkedHashMap<>(existing.children());
        for (RegistryCommandNode incomingChild : incoming.uniqueChildren()) {
            RegistryCommandNode existingChild = children.get(incomingChild.literal());
            if (existingChild == null) {
                registerAll(children, incomingChild.literals(), incomingChild, "subcommand already registered: ");
                continue;
            }

            RegistryCommandNode mergedChild = mergeNodes(existingChild, incomingChild);
            replaceNode(children, existingChild, mergedChild);
        }

        if (existing.isExecutable() && incoming.isExecutable()) {
            throw new IllegalArgumentException("command already registered: " + incoming.literal());
        }

        CommandRegistry.CommandExecutor executor = incoming.isExecutable() ? incoming.executor() : existing.executor();
        String description = mergeMetadata(existing.description(), incoming.description(), "description");
        String permission = mergeMetadata(existing.permission(), incoming.permission(), "permission");
        return new RegistryCommandNode(
            existing.literal(),
            description,
            permission,
            existing.aliases(),
            executor,
            arguments,
            options,
            children
        );
    }

    private static String mergeMetadata(String existing, String incoming, String label) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null || existing.equals(incoming)) {
            return existing;
        }
        throw new IllegalArgumentException("route conflicts with existing " + label);
    }

    private static <T> List<T> mergeSpecs(List<T> existing, List<T> incoming) {
        if (existing.isEmpty()) {
            return incoming;
        }
        if (incoming.isEmpty() || existing.equals(incoming)) {
            return existing;
        }
        throw new IllegalArgumentException("route conflicts with existing command shape");
    }

    private static void replaceNode(Map<String, RegistryCommandNode> nodes, RegistryCommandNode oldNode, RegistryCommandNode newNode) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, RegistryCommandNode> entry : nodes.entrySet()) {
            if (entry.getValue() == oldNode) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            nodes.put(key, newNode);
        }
    }
}
