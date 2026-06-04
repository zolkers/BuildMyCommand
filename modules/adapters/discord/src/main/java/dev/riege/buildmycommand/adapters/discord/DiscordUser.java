/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.discord;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record DiscordUser(
    String id,
    String username,
    Locale locale,
    Set<String> permissions,
    Object nativeHandle
) {
    public DiscordUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        locale = Objects.requireNonNullElse(locale, Locale.ROOT);
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }

    public static DiscordUser of(String id, String username, String... permissions) {
        return new DiscordUser(id, username, Locale.ROOT, Set.of(permissions), null);
    }
}
