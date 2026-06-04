/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RouteConflictAnalyzer {
    private RouteConflictAnalyzer() {
    }

    public static List<RouteConflict> findConflicts(Collection<RoutePattern> routes) {
        List<RoutePattern> routeList = List.copyOf(routes);
        List<RouteConflict> conflicts = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < routeList.size(); firstIndex++) {
            RoutePattern first = routeList.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < routeList.size(); secondIndex++) {
                RoutePattern second = routeList.get(secondIndex);
                if (overlaps(first, second)) {
                    conflicts.add(new RouteConflict(first, second, RouteCanonicalizer.canonical(first)));
                }
            }
        }
        return List.copyOf(conflicts);
    }

    private static boolean overlaps(RoutePattern first, RoutePattern second) {
        if (!literalLabelsOverlap(labels(first.rootLiteral(), first.rootAliases()),
            labels(second.rootLiteral(), second.rootAliases()))) {
            return false;
        }
        if (first.steps().size() != second.steps().size()) {
            return false;
        }
        for (int index = 0; index < first.steps().size(); index++) {
            if (!stepsOverlap(first.steps().get(index), second.steps().get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean stepsOverlap(RouteStep first, RouteStep second) {
        if (first instanceof LiteralRouteStep firstLiteral && second instanceof LiteralRouteStep secondLiteral) {
            return literalLabelsOverlap(labels(firstLiteral.value(), firstLiteral.aliases()),
                labels(secondLiteral.value(), secondLiteral.aliases()));
        }
        if (first instanceof ArgumentRouteStep firstArgument && second instanceof ArgumentRouteStep secondArgument) {
            return firstArgument.kind() == secondArgument.kind()
                && typesOverlap(firstArgument.type(), secondArgument.type());
        }
        if (first instanceof OptionRouteStep firstOption && second instanceof OptionRouteStep secondOption) {
            return firstOption.kind() == secondOption.kind()
                && typesOverlap(firstOption.type(), secondOption.type())
                && literalLabelsOverlap(labels(firstOption.name(), optionAlias(firstOption)),
                    labels(secondOption.name(), optionAlias(secondOption)));
        }
        return false;
    }

    private static List<String> optionAlias(OptionRouteStep option) {
        if (option.alias() == null) {
            return List.of();
        }
        return List.of(option.alias());
    }

    private static boolean typesOverlap(RouteType first, RouteType second) {
        if (first.inlineEnum() || second.inlineEnum()) {
            return first.inlineEnum()
                && second.inlineEnum()
                && literalLabelsOverlap(first.enumValues(), second.enumValues());
        }
        if (first.runtimeType() != second.runtimeType()) {
            return false;
        }
        return first.range() == null ? second.range() == null : first.range().equals(second.range());
    }

    private static List<String> labels(String primary, List<String> aliases) {
        List<String> labels = new ArrayList<>();
        labels.add(primary);
        labels.addAll(aliases);
        return labels;
    }

    private static boolean literalLabelsOverlap(List<String> first, List<String> second) {
        Set<String> firstLabels = new HashSet<>(first);
        for (String label : second) {
            if (firstLabels.contains(label)) {
                return true;
            }
        }
        return false;
    }
}
