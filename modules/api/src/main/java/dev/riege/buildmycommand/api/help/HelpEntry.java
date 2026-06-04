/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record HelpEntry(
    String path,
    String description,
    String group,
    List<String> aliases,
    Optional<String> permission
) {
    public HelpEntry {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(group, "group");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        permission = Objects.requireNonNull(permission, "permission");
    }
}
