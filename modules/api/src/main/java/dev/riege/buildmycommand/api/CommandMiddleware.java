package dev.riege.buildmycommand.api;

import java.util.List;

@FunctionalInterface
public interface CommandMiddleware {
    CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next);

    @FunctionalInterface
    interface Chain {
        CommandResult proceed(CommandContext context);
    }
}
