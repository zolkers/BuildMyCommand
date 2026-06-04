/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.CommandPlatform;

public final class TestPlatforms {
    private TestPlatforms() {
    }

    public static CommandPlatform fake(String id) {
        return new CommandPlatform(id, "Fake " + id, true, true, true);
    }

    public static CommandPlatform fake(
        String id,
        boolean supportsRichMessages,
        boolean supportsAutocomplete,
        boolean supportsPermissions
    ) {
        return new CommandPlatform(
            id,
            "Fake " + id,
            supportsRichMessages,
            supportsAutocomplete,
            supportsPermissions
        );
    }
}
