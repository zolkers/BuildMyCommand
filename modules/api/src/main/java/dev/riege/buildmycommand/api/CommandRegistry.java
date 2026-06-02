package dev.riege.buildmycommand.api;

import java.time.Duration;
import java.util.function.Consumer;

public interface CommandRegistry {
    default CommandRegistry caseInsensitiveLiterals() {
        throw new UnsupportedOperationException("case-insensitive literals are not supported by this registry");
    }

    default CommandRegistry caseInsensitiveOptions() {
        throw new UnsupportedOperationException("case-insensitive options are not supported by this registry");
    }

    void command(String literal, Consumer<CommandBuilder> configure);

    void register(CommandNode node);

    default boolean unregister(String path) {
        throw new UnsupportedOperationException("command unregister is not supported by this registry");
    }

    RouteBuilder route(String pattern);

    interface RouteBuilder {
        RouteBuilder description(String description);

        RouteBuilder permission(String permission);

        default RouteBuilder hidden() {
            throw new UnsupportedOperationException("hidden command metadata is not supported by this registry");
        }

        default RouteBuilder usage(String usage) {
            throw new UnsupportedOperationException("command usage metadata is not supported by this registry");
        }

        default RouteBuilder example(String example) {
            throw new UnsupportedOperationException("command example metadata is not supported by this registry");
        }

        default RouteBuilder cooldown(Duration cooldown) {
            throw new UnsupportedOperationException("command cooldown metadata is not supported by this registry");
        }

        default RouteBuilder requirement(String requirement) {
            throw new UnsupportedOperationException("command requirements are not supported by this registry");
        }

        default RouteBuilder group(String group) {
            throw new UnsupportedOperationException("command group metadata is not supported by this registry");
        }

        default RouteBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            throw new UnsupportedOperationException("argument suggestions are not supported by this registry");
        }

        default RouteBuilder argumentSuggestions(String name, String providerName, SuggestionProvider provider) {
            return argumentSuggestions(name, provider);
        }

        default RouteBuilder optionSuggestions(String name, SuggestionProvider provider) {
            throw new UnsupportedOperationException("option suggestions are not supported by this registry");
        }

        default RouteBuilder optionSuggestions(String name, String providerName, SuggestionProvider provider) {
            return optionSuggestions(name, provider);
        }

        CommandBuilder executes(CommandExecutor executor);
    }

    interface CommandBuilder {
        CommandBuilder description(String description);

        CommandBuilder permission(String permission);

        default CommandBuilder hidden() {
            throw new UnsupportedOperationException("hidden command metadata is not supported by this registry");
        }

        default CommandBuilder usage(String usage) {
            throw new UnsupportedOperationException("command usage metadata is not supported by this registry");
        }

        default CommandBuilder example(String example) {
            throw new UnsupportedOperationException("command example metadata is not supported by this registry");
        }

        default CommandBuilder cooldown(Duration cooldown) {
            throw new UnsupportedOperationException("command cooldown metadata is not supported by this registry");
        }

        default CommandBuilder requirement(String requirement) {
            throw new UnsupportedOperationException("command requirements are not supported by this registry");
        }

        default CommandBuilder group(String group) {
            throw new UnsupportedOperationException("command group metadata is not supported by this registry");
        }

        CommandBuilder alias(String alias);

        CommandBuilder aliases(String... aliases);

        CommandBuilder subcommand(String literal, Consumer<CommandBuilder> configure);

        <T> CommandBuilder argument(String name, Class<T> type);

        <T> CommandBuilder optionalArgument(String name, Class<T> type);

        <T> CommandBuilder greedyArgument(String name, Class<T> type);

        <T> CommandBuilder optionalGreedyArgument(String name, Class<T> type);

        CommandBuilder flag(String name);

        CommandBuilder flag(String name, String alias);

        <T> CommandBuilder option(String name, Class<T> type);

        <T> CommandBuilder option(String name, Class<T> type, String alias);

        default CommandBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            throw new UnsupportedOperationException("argument suggestions are not supported by this registry");
        }

        default CommandBuilder argumentSuggestions(String name, String providerName, SuggestionProvider provider) {
            return argumentSuggestions(name, provider);
        }

        default CommandBuilder optionSuggestions(String name, SuggestionProvider provider) {
            throw new UnsupportedOperationException("option suggestions are not supported by this registry");
        }

        default CommandBuilder optionSuggestions(String name, String providerName, SuggestionProvider provider) {
            return optionSuggestions(name, provider);
        }

        CommandBuilder executes(CommandExecutor executor);
    }

    @FunctionalInterface
    interface CommandExecutor {
        CommandResult execute(CommandContext context);
    }
}
