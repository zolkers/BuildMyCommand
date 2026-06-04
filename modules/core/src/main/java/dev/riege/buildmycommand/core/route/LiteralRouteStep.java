/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.route;


import java.util.List;

public record LiteralRouteStep(String value, List<String> aliases) implements RouteStep {
    public LiteralRouteStep {
        aliases = List.copyOf(aliases);
    }
}
