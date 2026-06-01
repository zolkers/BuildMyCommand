package dev.buildmycommand.api;

import java.util.function.Consumer;

public interface CommandRegistry {
    void command(String literal, Consumer<CommandBuilder> configure);

    interface CommandBuilder {
        void executes(CommandExecutor executor);
    }

    @FunctionalInterface
    interface CommandExecutor {
        CommandResult execute(CommandContext context);
    }
}
