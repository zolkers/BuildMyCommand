package dev.buildmycommand.api;

import java.util.Optional;

public final class Results {
    private static final CommandResult SILENT = new CommandResult(CommandResult.Status.SILENT, Optional.empty());

    private Results() {
    }

    public static CommandResult success(String reply) {
        return new CommandResult(CommandResult.Status.SUCCESS, Optional.of(reply));
    }

    public static CommandResult failure(String reply) {
        return new CommandResult(CommandResult.Status.FAILURE, Optional.of(reply));
    }

    public static CommandResult silent() {
        return SILENT;
    }
}
