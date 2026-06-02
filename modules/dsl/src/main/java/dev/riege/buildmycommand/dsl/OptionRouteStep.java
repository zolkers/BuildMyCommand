package dev.riege.buildmycommand.dsl;

import java.util.Objects;

public record OptionRouteStep(String name, RouteType type, String alias, RouteOptionKind kind) implements RouteStep {
    public OptionRouteStep {
        name = RouteParser.validateName(name, "option name");
        type = Objects.requireNonNull(type, "type");
        if (alias != null) {
            alias = RouteParser.validateName(alias, "option alias");
        }
        kind = Objects.requireNonNull(kind, "kind");
    }
}
