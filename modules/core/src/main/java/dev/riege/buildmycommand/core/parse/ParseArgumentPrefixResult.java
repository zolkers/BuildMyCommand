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

public record ParseArgumentPrefixResult(Map<String, Object> values, int consumed, Optional<String> failure) {
    static ParseArgumentPrefixResult success(Map<String, Object> values, int consumed) {
        return new ParseArgumentPrefixResult(Map.copyOf(values), consumed, Optional.empty());
    }

    static ParseArgumentPrefixResult failure(String failure) {
        return new ParseArgumentPrefixResult(Map.of(), 0, Optional.of(failure));
    }
}
