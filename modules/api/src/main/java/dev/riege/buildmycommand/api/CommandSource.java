package dev.riege.buildmycommand.api;

public interface CommandSource {
    default void reply(String message) {
    }

    default boolean hasPermission(String permission) {
        return true;
    }
}
