package dev.riege.buildmycommand.core.middleware;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;

import java.util.List;
import java.util.Objects;

public final class MiddlewareChain {
    private final List<CommandMiddleware> middleware;

    public MiddlewareChain(List<CommandMiddleware> middleware) {
        this.middleware = List.copyOf(Objects.requireNonNull(middleware, "middleware"));
    }

    public CommandResult execute(
        CommandContext context,
        CommandNode command,
        List<String> commandPath,
        CommandMiddleware.Chain terminal
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(commandPath, "commandPath");
        Objects.requireNonNull(terminal, "terminal");
        return proceed(0, context, command, List.copyOf(commandPath), terminal);
    }

    private CommandResult proceed(
        int index,
        CommandContext context,
        CommandNode command,
        List<String> commandPath,
        CommandMiddleware.Chain terminal
    ) {
        if (index >= middleware.size()) {
            return Objects.requireNonNull(terminal.proceed(context), "command result");
        }
        CommandMiddleware current = middleware.get(index);
        CommandResult result = current.execute(
            context,
            command,
            commandPath,
            nextContext -> proceed(index + 1, nextContext, command, commandPath, terminal)
        );
        return Objects.requireNonNull(result, "command result");
    }
}
