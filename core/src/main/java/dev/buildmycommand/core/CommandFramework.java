package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandContext;
import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;

import java.util.Objects;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;

    private CommandFramework(SimpleCommandRegistry registry) {
        this.registry = registry;
    }

    public static CommandFramework create() {
        return new CommandFramework(new SimpleCommandRegistry());
    }

    public CommandRegistry registry() {
        return registry;
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");

        CommandRegistry.CommandExecutor executor = registry.find(input);
        if (executor == null) {
            return Results.failure("Unknown command: " + input);
        }

        return Objects.requireNonNull(executor.execute(new CommandContext(source, input)), "command result");
    }
}
