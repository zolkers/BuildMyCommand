package dev.riege.buildmycommand.core;

import java.util.Optional;

record ParseResult<T>(T value, Optional<String> failure) {
    static <T> ParseResult<T> success(T value) {
        return new ParseResult<>(value, Optional.empty());
    }

    static <T> ParseResult<T> failure(String failure) {
        return new ParseResult<>(null, Optional.of(failure));
    }
}
