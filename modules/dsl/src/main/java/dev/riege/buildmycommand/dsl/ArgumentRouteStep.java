/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

import java.util.Objects;

public record ArgumentRouteStep(String name, RouteType type, RouteArgumentKind kind) implements RouteStep {
    public ArgumentRouteStep {
        name = RouteParser.validateName(name, "argument name");
        type = Objects.requireNonNull(type, "type");
        kind = Objects.requireNonNull(kind, "kind");
    }
}
