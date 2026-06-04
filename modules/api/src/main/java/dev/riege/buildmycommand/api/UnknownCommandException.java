/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Objects;

public final class UnknownCommandException extends CommandException {
    private final String command;

    public UnknownCommandException(String command) {
        super("Unknown command: " + Objects.requireNonNull(command, "command"));
        this.command = command;
    }

    public String command() {
        return command;
    }
}
