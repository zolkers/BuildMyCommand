/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.List;

@FunctionalInterface
public interface CommandMiddleware {
    CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next);

    @FunctionalInterface
    interface Chain {
        CommandResult proceed(CommandContext context);
    }
}
