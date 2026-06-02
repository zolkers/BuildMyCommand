package dev.riege.buildmycommand.core.route;


import dev.riege.buildmycommand.core.registry.SimpleCommandBuilder;
import dev.riege.buildmycommand.core.support.Validators;
public record LiteralRouteStep(String value) implements RouteStep {
}
