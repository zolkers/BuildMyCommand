package dev.riege.buildmycommand.core.route;


import dev.riege.buildmycommand.core.registry.SimpleCommandBuilder;
import dev.riege.buildmycommand.core.support.Validators;
import java.util.List;

public record RoutePattern(String rootLiteral, List<RouteStep> steps) {
    public RoutePattern {
        steps = List.copyOf(steps);
    }
}
