package dev.riege.buildmycommand.core;

import java.util.Map;
import java.util.Optional;

record ParseArgumentPrefixResult(Map<String, Object> values, int consumed, Optional<String> failure) {
    static ParseArgumentPrefixResult success(Map<String, Object> values, int consumed) {
        return new ParseArgumentPrefixResult(Map.copyOf(values), consumed, Optional.empty());
    }

    static ParseArgumentPrefixResult failure(String failure) {
        return new ParseArgumentPrefixResult(Map.of(), 0, Optional.of(failure));
    }
}
