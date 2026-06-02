package dev.riege.buildmycommand.api;

import java.util.Objects;
import java.util.Optional;

public final class CommandResult {
    private final Status status;
    private final Optional<CommandMessage> message;

    public CommandResult(Status status, Optional<String> reply) {
        this.status = Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reply, "reply");
        this.message = reply.map(text -> messageFor(status, text));
    }

    private CommandResult(Status status, CommandMessage message) {
        this.status = Objects.requireNonNull(status, "status");
        this.message = Optional.of(Objects.requireNonNull(message, "message"));
    }

    public static CommandResult message(Status status, CommandMessage message) {
        return new CommandResult(status, message);
    }

    public Status status() {
        return status;
    }

    public Optional<CommandMessage> message() {
        return message;
    }

    public Optional<String> reply() {
        return message.map(CommandMessage::text);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommandResult that)) {
            return false;
        }
        return status == that.status && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message);
    }

    @Override
    public String toString() {
        return "CommandResult[status=" + status + ", message=" + message + "]";
    }

    private static CommandMessage messageFor(Status status, String reply) {
        return switch (status) {
            case SUCCESS -> CommandMessage.success(reply);
            case FAILURE -> CommandMessage.error(reply);
            case SILENT -> CommandMessage.info(reply);
        };
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        SILENT
    }
}
