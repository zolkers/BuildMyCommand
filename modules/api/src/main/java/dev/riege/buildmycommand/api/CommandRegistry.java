package dev.riege.buildmycommand.api;

import java.util.function.Consumer;

public interface CommandRegistry {
    void command(String literal, Consumer<CommandBuilder> configure);

    void register(CommandNode node);

    RouteBuilder route(String pattern);

    interface RouteBuilder {
        RouteBuilder description(String description);

        RouteBuilder permission(String permission);

        CommandBuilder executes(CommandExecutor executor);
    }

    interface CommandBuilder {
        CommandBuilder description(String description);

        CommandBuilder permission(String permission);

        CommandBuilder alias(String alias);

        CommandBuilder aliases(String... aliases);

        CommandBuilder subcommand(String literal, Consumer<CommandBuilder> configure);

        <T> CommandBuilder argument(String name, Class<T> type);

        <T> CommandBuilder optionalArgument(String name, Class<T> type);

        <T> CommandBuilder greedyArgument(String name, Class<T> type);

        CommandBuilder flag(String name);

        CommandBuilder flag(String name, String alias);

        <T> CommandBuilder option(String name, Class<T> type);

        <T> CommandBuilder option(String name, Class<T> type, String alias);

        CommandBuilder executes(CommandExecutor executor);
    }

    @FunctionalInterface
    interface CommandExecutor {
        CommandResult execute(CommandContext context);
    }
}
