/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.Optional;

public record ParseResult<T>(T value, Optional<String> failure) {
    static <T> ParseResult<T> success(T value) {
        return new ParseResult<>(value, Optional.empty());
    }

    static <T> ParseResult<T> failure(String failure) {
        return new ParseResult<>(null, Optional.of(failure));
    }
}
