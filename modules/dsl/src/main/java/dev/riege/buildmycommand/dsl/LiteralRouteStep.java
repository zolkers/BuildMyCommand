package dev.riege.buildmycommand.dsl;

import java.util.List;

public record LiteralRouteStep(String value, List<String> aliases) implements RouteStep {
    public LiteralRouteStep {
        value = RouteParser.validateLiteral(value, "route literal");
        aliases = List.copyOf(aliases);
        aliases.forEach(alias -> RouteParser.validateLiteral(alias, "route literal alias"));
    }
}
