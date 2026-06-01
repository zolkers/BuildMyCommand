package dev.riege.buildmycommand.api;

import java.util.Optional;
import java.util.Objects;

public final class Results {
    private static final CommandResult SILENT = new CommandResult(CommandResult.Status.SILENT, Optional.empty());

    private Results() {
    }

    public static CommandResult success(String reply) {
        return new CommandResult(CommandResult.Status.SUCCESS, Optional.of(Objects.requireNonNull(reply, "reply")));
    }

    public static CommandResult failure(String reply) {
        return new CommandResult(CommandResult.Status.FAILURE, Optional.of(Objects.requireNonNull(reply, "reply")));
    }

    public static CommandResult silent() {
        return SILENT;
    }
}
