/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.PermissionSpec;

final class PermissionBinder {
    private PermissionBinder() {
    }

    static void apply(CommandRegistry.CommandBuilder builder, PermissionSpec permission) {
        if (permission.regex()) {
            builder.permissionRegex(permission.value());
        } else {
            builder.permission(permission.value());
        }
    }

    static void apply(CommandRegistry.RouteBuilder builder, PermissionSpec permission) {
        if (permission.regex()) {
            builder.permissionRegex(permission.value());
        } else {
            builder.permission(permission.value());
        }
    }
}
