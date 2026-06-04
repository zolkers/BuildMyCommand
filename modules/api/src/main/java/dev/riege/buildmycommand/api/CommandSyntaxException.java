/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

public final class CommandSyntaxException extends CommandException {
    public CommandSyntaxException(String message) {
        super(message);
    }
}
