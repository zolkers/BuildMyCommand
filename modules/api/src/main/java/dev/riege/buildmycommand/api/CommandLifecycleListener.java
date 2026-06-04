/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.List;

public interface CommandLifecycleListener {
    default void commandRegistered(CommandNode command, List<String> path) {
    }

    default void commandUpdated(CommandNode command, List<String> path) {
    }

    default void commandUnregistered(List<String> path) {
    }

    default void registryRebuilt(List<CommandNode> roots) {
    }
}
