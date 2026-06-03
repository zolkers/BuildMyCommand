package dev.riege.buildmycommand.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CommandExceptionContext(
    CommandInput input,
    Optional<CommandContext> context,
    Optional<CommandNode> command,
    List<String> commandPath
) {
    public CommandExceptionContext {
        Objects.requireNonNull(input, "input");
        context = Objects.requireNonNull(context, "context");
        command = Objects.requireNonNull(command, "command");
        commandPath = List.copyOf(commandPath);
    }

    public static CommandExceptionContext dispatch(CommandInput input) {
        return new CommandExceptionContext(input, Optional.empty(), Optional.empty(), List.of());
    }

    public static CommandExceptionContext execution(
        CommandInput input,
        CommandContext context,
        CommandNode command,
        List<String> commandPath
    ) {
        return new CommandExceptionContext(
            Objects.requireNonNull(input, "input"),
            Optional.of(Objects.requireNonNull(context, "context")),
            Optional.of(Objects.requireNonNull(command, "command")),
            commandPath
        );
    }
}
