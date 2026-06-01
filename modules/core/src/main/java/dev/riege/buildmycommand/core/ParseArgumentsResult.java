package dev.riege.buildmycommand.core;

import java.util.Map;
import java.util.Optional;

record ParseArgumentsResult(Map<String, Object> values, Optional<String> failure) {
    static ParseArgumentsResult success(Map<String, Object> values) {
        return new ParseArgumentsResult(Map.copyOf(values), Optional.empty());
    }

    static ParseArgumentsResult failure(String failure) {
        return new ParseArgumentsResult(Map.of(), Optional.of(failure));
    }
}
