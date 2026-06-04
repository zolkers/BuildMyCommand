/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.discord;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DiscordSlashCommand(
    String name,
    String description,
    List<String> aliases,
    List<DiscordSlashOption> options,
    List<DiscordSlashCommand> subcommands,
    Optional<String> permission
) {
    public DiscordSlashCommand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        subcommands = List.copyOf(Objects.requireNonNull(subcommands, "subcommands"));
        permission = Objects.requireNonNull(permission, "permission");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }
}
