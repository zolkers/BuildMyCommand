package dev.riege.buildmycommand.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public interface CommandSource {
    default Optional<String> id() {
        return Optional.empty();
    }

    default Optional<String> name() {
        return Optional.empty();
    }

    default Locale locale() {
        return Locale.ROOT;
    }

    default <T> Optional<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.empty();
    }

    default Optional<Object> metadata(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.empty();
    }

    default void reply(CommandMessage message) {
        Objects.requireNonNull(message, "message");
        reply(message.text());
    }

    default void reply(String message) {
    }

    default boolean hasPermission(String permission) {
        return true;
    }
}
