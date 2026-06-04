/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.dsl;

import java.util.List;
import java.util.Objects;

public record RouteType(String name, Class<?> runtimeType, List<String> enumValues, RouteRange range) {
    public RouteType {
        name = requireName(name, "route type");
        enumValues = List.copyOf(Objects.requireNonNull(enumValues, "enumValues"));
        if (runtimeType == null && enumValues.isEmpty()) {
            throw new IllegalArgumentException("route type must declare a runtime type or enum values");
        }
        if (!enumValues.isEmpty() && range != null) {
            throw new IllegalArgumentException("inline enum route type cannot declare a numeric range");
        }
    }

    public static RouteType runtime(String name, Class<?> runtimeType) {
        return new RouteType(name, Objects.requireNonNull(runtimeType, "runtimeType"), List.of(), null);
    }

    public static RouteType constrained(String name, Class<?> runtimeType, RouteRange range) {
        return new RouteType(name, Objects.requireNonNull(runtimeType, "runtimeType"), List.of(),
            Objects.requireNonNull(range, "range"));
    }

    public static RouteType inlineEnum(List<String> values) {
        return new RouteType("enum", null, values, null);
    }

    public boolean inlineEnum() {
        return !enumValues.isEmpty();
    }

    public boolean constrained() {
        return range != null;
    }

    public String displayName() {
        if (inlineEnum()) {
            return "enum(" + String.join(",", enumValues) + ")";
        }
        if (range != null) {
            return name + range.display();
        }
        return name;
    }

    private static String requireName(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
