package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public record ArgumentParseResult<T>(Optional<T> value, Optional<String> failure) {
    public ArgumentParseResult {
        value = Objects.requireNonNull(value, "value");
        failure = Objects.requireNonNull(failure, "failure");
        if (value.isPresent() == failure.isPresent()) {
            throw new IllegalArgumentException("parse result must contain exactly one value or failure");
        }
        failure.ifPresent(message -> {
            if (message.isBlank()) {
                throw new IllegalArgumentException("parse failure must not be blank");
            }
        });
    }

    public static <T> ArgumentParseResult<T> success(T value) {
        return new ArgumentParseResult<>(Optional.of(Objects.requireNonNull(value, "value")), Optional.empty());
    }

    public static <T> ArgumentParseResult<T> failure(String message) {
        return new ArgumentParseResult<>(Optional.empty(), Optional.of(Objects.requireNonNull(message, "message")));
    }
}
