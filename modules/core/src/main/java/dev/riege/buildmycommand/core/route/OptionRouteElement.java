/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.route;


import dev.riege.buildmycommand.core.registry.SimpleCommandBuilder;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.api.CommandRegistry;

public record OptionRouteElement(RouteElement delegate) implements RouteElement {
    @Override
    public void apply(CommandRegistry.CommandBuilder builder) {
        delegate.apply(builder);
    }
}
