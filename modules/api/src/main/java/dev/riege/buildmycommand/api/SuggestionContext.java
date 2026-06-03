package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Ergonomic view over {@link ArgumentParseContext} for user-defined completion providers.
 *
 * <p>The raw parse context remains available through {@link #parseContext()}, but this wrapper keeps
 * provider code readable when suggestions depend on the current source, input token, argument name,
 * or platform-specific metadata such as online players.</p>
 */
public record SuggestionContext(ArgumentParseContext parseContext) {
    public SuggestionContext {
        Objects.requireNonNull(parseContext, "parseContext");
    }

    public static SuggestionContext from(ArgumentParseContext parseContext) {
        return new SuggestionContext(parseContext);
    }

    public CommandSource source() {
        return parseContext.source();
    }

    public CommandInput input() {
        return parseContext.input();
    }

    public String name() {
        return parseContext.name();
    }

    public Class<?> type() {
        return parseContext.type();
    }

    public String currentToken() {
        return parseContext.rawToken();
    }

    public int replacementStart() {
        return parseContext.replacementStart();
    }

    public int replacementEnd() {
        return parseContext.replacementEnd();
    }

    public SuggestionType suggestionType() {
        return parseContext.suggestionType();
    }

    public Optional<Object> sourceMetadata(String key) {
        Objects.requireNonNull(key, "key");
        return source().metadata(key);
    }

    public <T> Optional<T> unwrapSource(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return source().unwrap(type);
    }
}
