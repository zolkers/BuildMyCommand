package dev.riege.buildmycommand.dsl;

import java.util.List;

public record RoutePattern(String rootLiteral, List<String> rootAliases, List<RouteStep> steps) {
    public RoutePattern {
        rootLiteral = RouteParser.validateLiteral(rootLiteral, "route literal");
        rootAliases = List.copyOf(rootAliases);
        rootAliases.forEach(alias -> RouteParser.validateLiteral(alias, "route literal alias"));
        steps = List.copyOf(steps);
    }
}
