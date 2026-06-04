/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

public final class Commands {
    private Commands() {
    }

    public static CommandNode.Builder literal(String literal) {
        return new CommandNode.Builder(literal);
    }
}
