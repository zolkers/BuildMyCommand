package dev.riege.buildmycommand.api;

import java.util.Objects;

public record ArgumentParseContext(
    CommandSource source,
    CommandInput input,
    String name,
    Class<?> type,
    String rawToken,
    int replacementStart,
    int replacementEnd,
    SuggestionType suggestionType
) {
    public ArgumentParseContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("argument or option name must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(suggestionType, "suggestionType");
        if (replacementStart < 0 || replacementEnd < replacementStart) {
            throw new IllegalArgumentException("invalid replacement range");
        }
    }
}
