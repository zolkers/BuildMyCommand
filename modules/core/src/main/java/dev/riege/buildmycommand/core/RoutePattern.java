package dev.riege.buildmycommand.core;

import java.util.List;

record RoutePattern(String rootLiteral, List<RouteStep> steps) {
    RoutePattern {
        steps = List.copyOf(steps);
    }
}
