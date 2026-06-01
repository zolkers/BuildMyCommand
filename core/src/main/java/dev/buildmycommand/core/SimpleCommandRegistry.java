package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.Results;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class SimpleCommandRegistry implements CommandRegistry {
    private final Map<String, CommandDefinition> commands = new HashMap<>();

    @Override
    public void command(String literal, Consumer<CommandBuilder> configure) {
        Objects.requireNonNull(literal, "literal");
        if (literal.isBlank()) {
            throw new IllegalArgumentException("literal must not be blank");
        }
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder();
        configure.accept(builder);
        if (commands.putIfAbsent(literal, builder.definition()) != null) {
            throw new IllegalArgumentException("command already registered: " + literal);
        }
    }

    CommandDefinition find(String literal) {
        return commands.get(literal);
    }

    private static final class SimpleCommandBuilder implements CommandBuilder {
        private final List<ArgumentSpec> arguments = new ArrayList<>();
        private CommandExecutor executor = context -> Results.silent();

        @Override
        public <T> CommandBuilder argument(String name, Class<T> type) {
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.REQUIRED));
            return this;
        }

        @Override
        public <T> CommandBuilder optionalArgument(String name, Class<T> type) {
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.OPTIONAL));
            return this;
        }

        @Override
        public <T> CommandBuilder greedyArgument(String name, Class<T> type) {
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.GREEDY));
            return this;
        }

        @Override
        public void executes(CommandExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
        }

        CommandDefinition definition() {
            return new CommandDefinition(executor, arguments);
        }
    }

    record CommandDefinition(CommandExecutor executor, List<ArgumentSpec> arguments) {
        CommandDefinition {
            Objects.requireNonNull(executor, "executor");
            arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        }
    }

    record ArgumentSpec(String name, Class<?> type, ArgumentKind kind) {
        ArgumentSpec {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("argument name must not be blank");
            }
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(kind, "kind");
        }
    }

    enum ArgumentKind {
        REQUIRED,
        OPTIONAL,
        GREEDY
    }
}
