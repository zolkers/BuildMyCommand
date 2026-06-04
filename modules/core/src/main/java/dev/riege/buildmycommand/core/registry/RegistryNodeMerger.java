/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.PermissionSpec;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;

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
        String duplicateMessage,
        CommandMatchingPolicy matchingPolicy
    ) {
        for (String literal : literals) {
            String key = matchingPolicy.literalKey(literal);
            if (nodes.containsKey(key)) {
                throw new IllegalArgumentException(duplicateMessage + literal);
            }
        }
        for (String literal : literals) {
            nodes.put(matchingPolicy.literalKey(literal), node);
        }
    }

    static void mergeRoot(
        Map<String, RegistryCommandNode> commands,
        RegistryCommandNode node,
        CommandMatchingPolicy matchingPolicy
    ) {
        RegistryCommandNode existing = commands.get(matchingPolicy.literalKey(node.literal()));
        if (existing == null) {
            registerAll(commands, node.literals(), node, "command already registered: ", matchingPolicy);
            return;
        }

        RegistryCommandNode merged = mergeNodes(existing, node, matchingPolicy);
        replaceNode(commands, existing, merged);
    }

    static void mergeChild(
        Map<String, RegistryCommandNode> children,
        RegistryCommandNode node,
        CommandMatchingPolicy matchingPolicy
    ) {
        RegistryCommandNode existing = children.get(matchingPolicy.literalKey(node.literal()));
        if (existing == null) {
            registerAll(children, node.literals(), node, "subcommand already registered: ", matchingPolicy);
            return;
        }

        RegistryCommandNode merged = mergeNodes(existing, node, matchingPolicy);
        replaceNode(children, existing, merged);
    }

    private static RegistryCommandNode mergeNodes(
        RegistryCommandNode existing,
        RegistryCommandNode incoming,
        CommandMatchingPolicy matchingPolicy
    ) {
        if (!matchingPolicy.literalKey(existing.literal()).equals(matchingPolicy.literalKey(incoming.literal()))) {
            throw new IllegalArgumentException("cannot merge different literals: " + incoming.literal());
        }

        List<RegistryArgumentSpec> arguments = mergeSpecs(existing.arguments(), incoming.arguments());
        List<RegistryOptionSpec> options = mergeSpecs(existing.options(), incoming.options());
        Map<String, RegistryCommandNode> children = new LinkedHashMap<>(existing.children());
        for (RegistryCommandNode incomingChild : incoming.uniqueChildren()) {
            RegistryCommandNode existingChild = children.get(matchingPolicy.literalKey(incomingChild.literal()));
            if (existingChild == null) {
                registerAll(children, incomingChild.literals(), incomingChild, "subcommand already registered: ", matchingPolicy);
                continue;
            }

            RegistryCommandNode mergedChild = mergeNodes(existingChild, incomingChild, matchingPolicy);
            replaceNode(children, existingChild, mergedChild);
        }

        if (existing.isExecutable() && incoming.isExecutable()) {
            throw new IllegalArgumentException("command already registered: " + incoming.literal());
        }

        CommandRegistry.CommandExecutor executor = incoming.isExecutable() ? incoming.executor() : existing.executor();
        String description = mergeMetadata(existing.description(), incoming.description(), "description");
        PermissionSpec permission = mergePermission(existing.permissionSpec(), incoming.permissionSpec());
        return new RegistryCommandNode(
            existing.literal(),
            description,
            permission == null ? null : permission.value(),
            permission,
            existing.aliases(),
            executor,
            arguments,
            options,
            mergeCommandMetadata(existing.metadata(), incoming.metadata()),
            children
        );
    }

    private static PermissionSpec mergePermission(PermissionSpec existing, PermissionSpec incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null || existing.equals(incoming)) {
            return existing;
        }
        throw new IllegalArgumentException("route conflicts with existing permission");
    }

    private static CommandMetadata mergeCommandMetadata(CommandMetadata existing, CommandMetadata incoming) {
        if (existing.equals(CommandMetadata.empty())) {
            return incoming;
        }
        if (incoming.equals(CommandMetadata.empty()) || existing.equals(incoming)) {
            return existing;
        }
        if (onlyAliasSuggestionDiffers(existing, incoming)) {
            return new CommandMetadata(
                existing.hidden(),
                existing.usage(),
                existing.examples(),
                existing.cooldown(),
                existing.requirement(),
                existing.group(),
                incoming.suggestAliases(),
                existing.middlewares()
            );
        }
        throw new IllegalArgumentException("route conflicts with existing command metadata");
    }

    private static boolean onlyAliasSuggestionDiffers(CommandMetadata existing, CommandMetadata incoming) {
        return existing.hidden() == incoming.hidden()
            && existing.usage().equals(incoming.usage())
            && existing.group().equals(incoming.group())
            && existing.examples().equals(incoming.examples())
            && existing.cooldown().equals(incoming.cooldown())
            && existing.requirement().equals(incoming.requirement())
            && existing.middlewares().equals(incoming.middlewares());
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
