/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Describes the permission guard attached to a command node.
 *
 * <p>Exact permissions are the default and map to
 * {@link CommandSource#hasPermission(String)}. Regex permissions are opt-in and
 * map to {@link CommandSource#hasPermissionMatching(Pattern)}.</p>
 *
 * <pre>{@code
 * PermissionSpec exact = PermissionSpec.exact("admin.reload");
 * PermissionSpec regex = PermissionSpec.regex("admin\\..*");
 * }</pre>
 */
public record PermissionSpec(String value, boolean regex) {
    public PermissionSpec {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("permission must not be blank");
        }
        if (regex) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException exception) {
                throw new IllegalArgumentException("invalid permission regex: " + value, exception);
            }
        }
    }

    public static PermissionSpec exact(String permission) {
        return new PermissionSpec(permission, false);
    }

    public static PermissionSpec regex(String pattern) {
        return new PermissionSpec(pattern, true);
    }

    public Pattern pattern() {
        if (!regex) {
            throw new IllegalStateException("exact permission does not have a regex pattern");
        }
        return Pattern.compile(value);
    }

    public String display() {
        return regex ? "/" + value + "/" : value;
    }
}
