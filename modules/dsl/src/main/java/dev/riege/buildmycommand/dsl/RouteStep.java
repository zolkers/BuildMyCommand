/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

public sealed interface RouteStep permits LiteralRouteStep, ArgumentRouteStep, OptionRouteStep {
}
