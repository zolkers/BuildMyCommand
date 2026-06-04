/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.PermissionSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RegistryCommandNode(
    String literal,
    String description,
    String permission,
    PermissionSpec permissionSpec,
    List<String> aliases,
    CommandRegistry.CommandExecutor executor,
    List<RegistryArgumentSpec> arguments,
    List<RegistryOptionSpec> options,
    CommandMetadata metadata,
    Map<String, RegistryCommandNode> children
) {
    public RegistryCommandNode {
        Objects.requireNonNull(literal, "literal");
        if (description != null && description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (permission != null && permission.isBlank()) {
            throw new IllegalArgumentException("permission must not be blank");
        }
        if (permissionSpec != null && !permissionSpec.value().equals(permission)) {
            throw new IllegalArgumentException("permission must match permissionSpec value");
        }
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(executor, "executor");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        metadata = Objects.requireNonNull(metadata, "metadata");
        children = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(children, "children")));
    }

    public RegistryCommandNode(
        String literal,
        String description,
        String permission,
        List<String> aliases,
        CommandRegistry.CommandExecutor executor,
        List<RegistryArgumentSpec> arguments,
        List<RegistryOptionSpec> options,
        CommandMetadata metadata,
        Map<String, RegistryCommandNode> children
    ) {
        this(
            literal,
            description,
            permission,
            permission == null ? null : PermissionSpec.exact(permission),
            aliases,
            executor,
            arguments,
            options,
            metadata,
            children
        );
    }

    public List<String> literals() {
        List<String> literals = new ArrayList<>(aliases.size() + 1);
        literals.add(literal);
        literals.addAll(aliases);
        return literals;
    }

    public Optional<String> descriptionOptional() {
        return Optional.ofNullable(description);
    }

    public Optional<String> permissionOptional() {
        return Optional.ofNullable(permission);
    }

    public Optional<PermissionSpec> permissionSpecOptional() {
        return Optional.ofNullable(permissionSpec);
    }

    public boolean isExecutable() {
        return executor != SimpleCommandRegistry.DEFAULT_EXECUTOR;
    }

    public List<RegistryCommandNode> uniqueChildren() {
        List<RegistryCommandNode> unique = new ArrayList<>();
        for (RegistryCommandNode child : children.values()) {
            if (!unique.contains(child)) {
                unique.add(child);
            }
        }
        return unique;
    }
}
