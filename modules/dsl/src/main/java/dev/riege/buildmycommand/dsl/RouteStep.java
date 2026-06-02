package dev.riege.buildmycommand.dsl;

public sealed interface RouteStep permits LiteralRouteStep, ArgumentRouteStep, OptionRouteStep {
}
