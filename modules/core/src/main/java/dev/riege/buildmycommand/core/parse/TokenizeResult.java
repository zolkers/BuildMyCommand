/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.List;
import java.util.Optional;

public record TokenizeResult(List<String> tokens, Optional<String> failure) {
    static TokenizeResult success(List<String> tokens) {
        return new TokenizeResult(List.copyOf(tokens), Optional.empty());
    }

    static TokenizeResult failure(String failure) {
        return new TokenizeResult(List.of(), Optional.of(failure));
    }
}
