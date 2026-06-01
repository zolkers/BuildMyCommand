package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.Results;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class SimpleCommandRegistry implements CommandRegistry {
    private final Map<String, CommandExecutor> commands = new HashMap<>();

    @Override
    public void command(String literal, Consumer<CommandBuilder> configure) {
        Objects.requireNonNull(literal, "literal");
        if (literal.isBlank()) {
            throw new IllegalArgumentException("literal must not be blank");
        }
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder();
        configure.accept(builder);
        if (commands.putIfAbsent(literal, builder.executor()) != null) {
            throw new IllegalArgumentException("command already registered: " + literal);
        }
    }

    CommandExecutor find(String input) {
        return commands.get(input);
    }

    private static final class SimpleCommandBuilder implements CommandBuilder {
        private CommandExecutor executor = context -> Results.silent();

        @Override
        public void executes(CommandExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
        }

        CommandExecutor executor() {
            return executor;
        }
    }
}
