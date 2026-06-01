package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public record CommandResult(Status status, Optional<String> reply) {
    public CommandResult {
        Objects.requireNonNull(status, "status");
        reply = Objects.requireNonNull(reply, "reply");
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        SILENT
    }
}
