/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

import java.util.ArrayList;
import java.util.List;

public final class RouteCanonicalizer {
    private RouteCanonicalizer() {
    }

    public static String display(RoutePattern route) {
        List<String> parts = new ArrayList<>();
        parts.add(displayLiteral(route.rootLiteral(), route.rootAliases()));
        route.steps().stream().map(RouteCanonicalizer::displayStep).forEach(parts::add);
        return String.join(" ", parts);
    }

    public static String displayStep(RouteStep step) {
        return switch (step) {
            case LiteralRouteStep literal -> displayLiteral(literal.value(), literal.aliases());
            case ArgumentRouteStep argument -> displayArgument(argument);
            case OptionRouteStep option -> displayOption(option);
        };
    }

    public static String canonical(RoutePattern route) {
        List<String> parts = new ArrayList<>();
        parts.add(canonicalLiteral(route.rootLiteral(), route.rootAliases()));
        route.steps().stream().map(RouteCanonicalizer::canonicalStep).forEach(parts::add);
        return String.join(" ", parts);
    }

    public static String canonicalStep(RouteStep step) {
        return switch (step) {
            case LiteralRouteStep literal -> canonicalLiteral(literal.value(), literal.aliases());
            case ArgumentRouteStep argument -> displayArgument(argument);
            case OptionRouteStep option -> displayOption(option);
        };
    }

    private static String displayLiteral(String value, List<String> aliases) {
        List<String> labels = new ArrayList<>();
        labels.add(value);
        labels.addAll(aliases);
        return String.join("|", labels);
    }

    private static String canonicalLiteral(String value, List<String> aliases) {
        List<String> labels = new ArrayList<>();
        labels.add(value);
        labels.addAll(aliases);
        labels.sort(String::compareTo);
        return String.join("|", labels);
    }

    private static String displayArgument(ArgumentRouteStep argument) {
        String suffix = switch (argument.kind()) {
            case REQUIRED, OPTIONAL -> "";
            case GREEDY, OPTIONAL_GREEDY -> "...";
        };
        String token = argument.name() + ":" + argument.type().displayName() + suffix;
        return switch (argument.kind()) {
            case REQUIRED, GREEDY -> "<" + token + ">";
            case OPTIONAL, OPTIONAL_GREEDY -> "[" + token + "]";
        };
    }

    private static String displayOption(OptionRouteStep option) {
        String token = "--" + option.name();
        if (option.kind() == RouteOptionKind.VALUE) {
            token += ":" + option.type().displayName();
        }
        if (option.alias() != null) {
            token += "|-" + option.alias();
        }
        return "[" + token + "]";
    }
}
