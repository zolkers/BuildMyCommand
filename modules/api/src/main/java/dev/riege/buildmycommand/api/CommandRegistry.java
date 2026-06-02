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

    RouteBuilder route(String pattern);

    interface RouteBuilder {
        RouteBuilder description(String description);

        RouteBuilder permission(String permission);

        default RouteBuilder hidden() {
            return this;
        }

        default RouteBuilder usage(String usage) {
            return this;
        }

        default RouteBuilder example(String example) {
            return this;
        }

        default RouteBuilder cooldown(Duration cooldown) {
            return this;
        }

        default RouteBuilder requirement(String requirement) {
            return this;
        }

        default RouteBuilder group(String group) {
            return this;
        }

        default RouteBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            return this;
        }

        default RouteBuilder argumentSuggestions(String name, String providerName, SuggestionProvider provider) {
            return argumentSuggestions(name, provider);
        }

        default RouteBuilder optionSuggestions(String name, SuggestionProvider provider) {
            return this;
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
            return this;
        }

        default CommandBuilder usage(String usage) {
            return this;
        }

        default CommandBuilder example(String example) {
            return this;
        }

        default CommandBuilder cooldown(Duration cooldown) {
            return this;
        }

        default CommandBuilder requirement(String requirement) {
            return this;
        }

        default CommandBuilder group(String group) {
            return this;
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
            return this;
        }

        default CommandBuilder argumentSuggestions(String name, String providerName, SuggestionProvider provider) {
            return argumentSuggestions(name, provider);
        }

        default CommandBuilder optionSuggestions(String name, SuggestionProvider provider) {
            return this;
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
