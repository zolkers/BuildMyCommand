/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;

import java.util.List;

final class AnnotationRouteAliases {
    private AnnotationRouteAliases() {
    }

    static String aliasedRoute(String route, Alias... aliases) {
        String aliased = route;
        for (Alias alias : aliases) {
            if (alias == null) {
                continue;
            }
            for (String value : alias.value()) {
                aliased = applyRouteAlias(aliased, value, 0);
            }
        }
        return aliased;
    }

    static String aliasedSubcommandRoute(
        String route,
        List<String> ownerAliases,
        Alias methodAlias,
        int methodAliasOffset
    ) {
        String aliased = route;
        for (String value : ownerAliases) {
            aliased = applyRouteAlias(aliased, value, 0);
        }
        if (methodAlias != null) {
            for (String value : methodAlias.value()) {
                aliased = applyRouteAlias(aliased, value, methodAliasOffset);
            }
        }
        return aliased;
    }

    private static String applyRouteAlias(String route, String alias, int offset) {
        String[] routeTokens = route.trim().split("\\s+");
        String[] aliasTokens = alias.trim().split("\\s+");
        if (alias.isBlank()) {
            throw new IllegalArgumentException("route alias must not be blank");
        }
        if (offset + aliasTokens.length > routeTokens.length) {
            throw new IllegalArgumentException("route alias is longer than route: " + alias);
        }

        String[] updated = routeTokens.clone();
        for (int index = 0; index < aliasTokens.length; index++) {
            int routeIndex = offset + index;
            if (routeTokens[routeIndex].startsWith("<") || routeTokens[routeIndex].startsWith("[")
                || routeTokens[routeIndex].startsWith("--")) {
                throw new IllegalArgumentException("route alias can only target literal tokens: " + alias);
            }
            updated[routeIndex] = routeTokens[routeIndex] + "|" + aliasTokens[index];
        }
        return String.join(" ", updated);
    }
}
