/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Platform-independent view of the actor executing a command.
 *
 * <p>Adapters usually wrap a native sender/player/session in this interface so
 * commands can stay independent from the host runtime. Exact permission checks
 * call {@link #hasPermission(String)}. Regex permission checks call
 * {@link #hasPermissionMatching(Pattern)}, whose default implementation searches
 * the enumerable values returned by {@link #permissions()}.</p>
 *
 * <pre>{@code
 * public Set<String> permissions() {
 *     return Set.copyOf(effectivePermissions);
 * }
 * }</pre>
 */
public interface CommandSource {
    default Optional<String> id() {
        return Optional.empty();
    }

    default Optional<String> name() {
        return Optional.empty();
    }

    default Locale locale() {
        return Locale.ROOT;
    }

    default <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.empty();
    }

    default Optional<Object> metadata(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.empty();
    }

    default void reply(CommandMessage message) {
        Objects.requireNonNull(message, "message");
        reply(message.text());
    }

    default void reply(String message) {
    }

    default boolean hasPermission(String permission) {
        return true;
    }

    default Set<String> permissions() {
        return Set.of();
    }

    default boolean hasPermissionMatching(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return permissions().stream().anyMatch(permission -> pattern.matcher(permission).matches());
    }

    default boolean hasPermissionSpec(PermissionSpec permission) {
        Objects.requireNonNull(permission, "permission");
        if (!permission.regex()) {
            return hasPermission(permission.value());
        }
        return hasPermissionMatching(permission.pattern());
    }
}
