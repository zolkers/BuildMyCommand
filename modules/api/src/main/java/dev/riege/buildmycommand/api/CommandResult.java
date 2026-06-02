package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public record CommandResult(Status status, Optional<CommandMessage> message) {
    public CommandResult {
        Objects.requireNonNull(status, "status");
        message = Objects.requireNonNull(message, "message");
    }

    public Optional<String> reply() {
        return message.map(CommandMessage::text);
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        SILENT
    }
}
