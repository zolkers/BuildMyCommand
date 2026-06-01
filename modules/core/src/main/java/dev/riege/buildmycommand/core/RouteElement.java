package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandRegistry;

@FunctionalInterface
interface RouteElement {
    void apply(CommandRegistry.CommandBuilder builder);
}
