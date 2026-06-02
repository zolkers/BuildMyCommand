package dev.riege.buildmycommand.api;

import java.util.List;

@FunctionalInterface
public interface CommandErrorHandler {
    CommandResult handle(CommandContext context, CommandNode command, List<String> commandPath, Throwable error);
}
