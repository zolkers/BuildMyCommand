/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.support;

import java.util.Objects;

public final class Validators {
    private Validators() {
    }

    public static String literal(String literal, String label) {
        Objects.requireNonNull(literal, label);
        if (literal.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return literal;
    }

    public static String metadata(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    public static String name(String name, String label) {
        Objects.requireNonNull(name, label);
        if (name.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '-' && character != '_') {
                throw new IllegalArgumentException("invalid " + label + ": " + name);
            }
        }
        return name;
    }
}
