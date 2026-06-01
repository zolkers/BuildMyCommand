package dev.riege.buildmycommand.core;

sealed interface RouteStep permits LiteralRouteStep, ElementRouteStep {
}
