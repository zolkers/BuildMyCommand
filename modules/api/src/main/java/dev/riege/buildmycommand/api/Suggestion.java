package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public record Suggestion(
    String value,
    Optional<String> tooltip,
    int replacementStart,
    int replacementEnd,
    SuggestionType type,
    int priority
) {
    public Suggestion {
        Objects.requireNonNull(value, "value");
        tooltip = Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(type, "type");
        if (replacementStart < 0 || replacementEnd < replacementStart) {
            throw new IllegalArgumentException("invalid suggestion replacement range");
        }
    }
}
