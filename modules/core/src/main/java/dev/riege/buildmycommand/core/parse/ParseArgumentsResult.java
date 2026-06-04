/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.parse;


import dev.riege.buildmycommand.core.registry.*;
import java.util.Map;
import java.util.Optional;

public record ParseArgumentsResult(Map<String, Object> values, Optional<String> failure) {
    static ParseArgumentsResult success(Map<String, Object> values) {
        return new ParseArgumentsResult(Map.copyOf(values), Optional.empty());
    }

    static ParseArgumentsResult failure(String failure) {
        return new ParseArgumentsResult(Map.of(), Optional.of(failure));
    }
}
