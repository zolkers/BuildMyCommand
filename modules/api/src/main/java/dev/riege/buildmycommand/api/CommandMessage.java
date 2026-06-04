/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Map;
import java.util.Objects;

public record CommandMessage(String text, MessageLevel level, Map<String, Object> metadata) {
    public CommandMessage {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(level, "level");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public static CommandMessage info(String text) {
        return new CommandMessage(text, MessageLevel.INFO, Map.of());
    }

    public static CommandMessage success(String text) {
        return new CommandMessage(text, MessageLevel.SUCCESS, Map.of());
    }

    public static CommandMessage error(String text) {
        return new CommandMessage(text, MessageLevel.ERROR, Map.of());
    }
}
