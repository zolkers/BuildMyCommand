package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandRegistry;

record OptionRouteElement(RouteElement delegate) implements RouteElement {
    @Override
    public void apply(CommandRegistry.CommandBuilder builder) {
        delegate.apply(builder);
    }
}
