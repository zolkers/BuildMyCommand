/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Objects;

public record CommandPlatform(
    String id,
    String displayName,
    boolean supportsRichMessages,
    boolean supportsAutocomplete,
    boolean supportsPermissions
) {
    public CommandPlatform {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isBlank()) {
            throw new IllegalArgumentException("platform id must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("platform display name must not be blank");
        }
    }

    public static CommandPlatform test() {
        return new CommandPlatform("test", "Test", false, true, true);
    }

    public static CommandPlatform terminal() {
        return new CommandPlatform("terminal", "Terminal", false, true, true);
    }
}
