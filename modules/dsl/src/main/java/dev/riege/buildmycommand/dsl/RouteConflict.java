/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

public record RouteConflict(RoutePattern first, RoutePattern second, String canonicalRoute) {
}
