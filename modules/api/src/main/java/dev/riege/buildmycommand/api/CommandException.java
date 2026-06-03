package dev.riege.buildmycommand.api;

import java.util.Objects;

public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(validateMessage(message));
    }

    public CommandException(String message, Throwable cause) {
        super(validateMessage(message), Objects.requireNonNull(cause, "cause"));
    }

    static String validateMessage(String message) {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }
}
