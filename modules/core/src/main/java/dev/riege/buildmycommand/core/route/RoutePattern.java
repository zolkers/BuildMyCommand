/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.route;


import java.util.List;

public record RoutePattern(String rootLiteral, List<String> rootAliases, List<RouteStep> steps) {
    public RoutePattern {
        rootAliases = List.copyOf(rootAliases);
        steps = List.copyOf(steps);
    }
}
