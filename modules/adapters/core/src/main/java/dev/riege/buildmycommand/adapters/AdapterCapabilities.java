/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandPlatform;

import java.util.Objects;

public record AdapterCapabilities(
    boolean supportsRichMessages,
    boolean supportsAutocomplete,
    boolean supportsPermissions
) {
    public static AdapterCapabilities from(CommandPlatform platform) {
        Objects.requireNonNull(platform, "platform");
        return new AdapterCapabilities(
            platform.supportsRichMessages(),
            platform.supportsAutocomplete(),
            platform.supportsPermissions()
        );
    }
}
