package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record RegistryCommandNode(
    String literal,
    String description,
    String permission,
    List<String> aliases,
    CommandRegistry.CommandExecutor executor,
    List<RegistryArgumentSpec> arguments,
    List<RegistryOptionSpec> options,
    Map<String, RegistryCommandNode> children
) {
    RegistryCommandNode {
        Objects.requireNonNull(literal, "literal");
        if (description != null && description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (permission != null && permission.isBlank()) {
            throw new IllegalArgumentException("permission must not be blank");
        }
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(executor, "executor");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        children = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(children, "children")));
    }

    List<String> literals() {
        List<String> literals = new ArrayList<>(aliases.size() + 1);
        literals.add(literal);
        literals.addAll(aliases);
        return literals;
    }

    Optional<String> descriptionOptional() {
        return Optional.ofNullable(description);
    }

    Optional<String> permissionOptional() {
        return Optional.ofNullable(permission);
    }

    boolean isExecutable() {
        return executor != SimpleCommandRegistry.DEFAULT_EXECUTOR;
    }

    List<RegistryCommandNode> uniqueChildren() {
        List<RegistryCommandNode> unique = new ArrayList<>();
        for (RegistryCommandNode child : children.values()) {
            if (!unique.contains(child)) {
                unique.add(child);
            }
        }
        return unique;
    }
}
