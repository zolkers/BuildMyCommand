/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.discord;

import java.util.Objects;

public record DiscordTextCommand(String content, int cursor) {
    public DiscordTextCommand {
        Objects.requireNonNull(content, "content");
        cursor = Math.max(0, Math.min(cursor, content.length()));
    }

    public static DiscordTextCommand of(String content) {
        return new DiscordTextCommand(content, content.length());
    }
}
