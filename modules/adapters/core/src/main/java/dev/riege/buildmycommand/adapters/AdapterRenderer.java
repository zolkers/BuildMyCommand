/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandResult;

import java.util.Objects;

@FunctionalInterface
public interface AdapterRenderer<R> {
    R render(CommandResult result);

    static AdapterRenderer<CommandResult> identity() {
        return result -> Objects.requireNonNull(result, "result");
    }
}
