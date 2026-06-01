package dev.buildmycommand.api;

public interface CommandSource {
    default void reply(String message) {
    }
}
