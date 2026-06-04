/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.route;


import dev.riege.buildmycommand.core.registry.SimpleCommandBuilder;
import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteOptionKind;
import dev.riege.buildmycommand.dsl.RouteParser;
import dev.riege.buildmycommand.dsl.RouteType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RoutePatternParser {
    private RoutePatternParser() {
    }

    /**
     * Compatibility facade for core callers; public DSL parsing lives in the :dsl module.
     */
    public static RoutePattern parse(String pattern) {
        return parse(pattern, Map.of());
    }

    public static RoutePattern parse(String pattern, Map<String, Class<?>> customTypes) {
        dev.riege.buildmycommand.dsl.RoutePattern parsed = RouteParser.parse(pattern, customTypes);
        List<RouteStep> steps = new ArrayList<>();
        for (dev.riege.buildmycommand.dsl.RouteStep step : parsed.steps()) {
            steps.add(convertStep(step));
        }
        return new RoutePattern(parsed.rootLiteral(), parsed.rootAliases(), steps);
    }

    private static RouteStep convertStep(dev.riege.buildmycommand.dsl.RouteStep step) {
        Objects.requireNonNull(step, "step");
        return switch (step) {
            case dev.riege.buildmycommand.dsl.LiteralRouteStep literal ->
                new LiteralRouteStep(literal.value(), literal.aliases());
            case ArgumentRouteStep argument -> new ElementRouteStep(convertArgument(argument));
            case OptionRouteStep option -> new ElementRouteStep(new OptionRouteElement(convertOption(option)));
        };
    }

    private static RouteElement convertArgument(ArgumentRouteStep argument) {
        Class<?> type = runtimeType(argument.type());
        return switch (argument.kind()) {
            case REQUIRED -> builder -> builder.argument(argument.name(), typedClass(type));
            case OPTIONAL -> builder -> builder.optionalArgument(argument.name(), typedClass(type));
            case GREEDY -> builder -> builder.greedyArgument(argument.name(), typedClass(type));
            case OPTIONAL_GREEDY -> builder -> ((SimpleCommandBuilder) builder)
                .optionalGreedyArgument(argument.name(), typedClass(type));
        };
    }

    private static RouteElement convertOption(OptionRouteStep option) {
        Class<?> type = runtimeType(option.type());
        if (option.kind() == RouteOptionKind.FLAG) {
            return builder -> builder.flag(option.name(), option.alias());
        }
        return builder -> builder.option(option.name(), typedClass(type), option.alias());
    }

    private static Class<?> runtimeType(RouteType type) {
        if (type.inlineEnum()) {
            throw new IllegalArgumentException("inline enum route types are supported by DSL analysis only: "
                + type.displayName());
        }
        if (type.constrained()) {
            throw new IllegalArgumentException("constrained route types are supported by DSL analysis only: "
                + type.displayName());
        }
        return type.runtimeType();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> typedClass(Class<?> type) {
        return (Class<T>) type;
    }
}
