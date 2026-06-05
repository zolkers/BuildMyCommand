/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api;

import java.util.Objects;

public record ArgumentParseContext(
    CommandSource source,
    CommandInput input,
    String name,
    Class<?> type,
    String rawToken,
    int replacementStart,
    int replacementEnd,
    SuggestionType suggestionType,
    String currentInput,
    int currentInputStart
) {
    public ArgumentParseContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument or option name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(suggestionType, "suggestionType");
        Objects.requireNonNull(currentInput, "currentInput");
        if (replacementStart < 0 || replacementEnd < replacementStart) {
            throw new IllegalArgumentException("invalid replacement range");
        }
        if (currentInputStart < 0) {
            throw new IllegalArgumentException("current input start must not be negative");
        }
    }

    public ArgumentParseContext(
        CommandSource source,
        CommandInput input,
        String name,
        Class<?> type,
        String rawToken,
        int replacementStart,
        int replacementEnd,
        SuggestionType suggestionType
    ) {
        this(source, input, name, type, rawToken, replacementStart, replacementEnd, suggestionType, rawToken,
            replacementStart);
    }
}
