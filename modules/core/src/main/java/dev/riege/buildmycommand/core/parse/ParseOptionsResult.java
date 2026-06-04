/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ParseOptionsResult(Map<String, Object> values, List<String> positionals, Optional<String> failure) {
    static ParseOptionsResult success(Map<String, Object> values, List<String> positionals) {
        return new ParseOptionsResult(Map.copyOf(values), List.copyOf(positionals), Optional.empty());
    }

    static ParseOptionsResult failure(String failure) {
        return new ParseOptionsResult(Map.of(), List.of(), Optional.of(failure));
    }
}
