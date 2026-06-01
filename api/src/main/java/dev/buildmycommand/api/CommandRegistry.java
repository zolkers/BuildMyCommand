package dev.buildmycommand.api;

import java.util.function.Consumer;

public interface CommandRegistry {
    void command(String literal, Consumer<CommandBuilder> configure);

    interface CommandBuilder {
        <T> CommandBuilder argument(String name, Class<T> type);

        <T> CommandBuilder optionalArgument(String name, Class<T> type);

        <T> CommandBuilder greedyArgument(String name, Class<T> type);

        void executes(CommandExecutor executor);
    }

    @FunctionalInterface
    interface CommandExecutor {
        CommandResult execute(CommandContext context);
    }
}
