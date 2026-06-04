/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

@FunctionalInterface
public interface CommandExceptionHandler {
    CommandResult handle(CommandExceptionContext context, Throwable error);
}
