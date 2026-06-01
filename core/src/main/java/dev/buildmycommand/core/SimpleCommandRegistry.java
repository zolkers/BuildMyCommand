package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.Results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class SimpleCommandRegistry implements CommandRegistry {
    private final Map<String, CommandNode> commands = new LinkedHashMap<>();

    @Override
    public void command(String literal, Consumer<CommandBuilder> configure) {
        Objects.requireNonNull(literal, "literal");
        if (literal.isBlank()) {
            throw new IllegalArgumentException("literal must not be blank");
        }
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder(literal);
        configure.accept(builder);
        CommandNode node = builder.node();
        registerAll(commands, node.literals(), node, "command already registered: ");
    }

    CommandNode find(String literal) {
        return commands.get(literal);
    }

    List<CommandNode> roots() {
        List<CommandNode> roots = new ArrayList<>();
        for (CommandNode command : commands.values()) {
            if (!roots.contains(command)) {
                roots.add(command);
            }
        }
        return roots;
    }

    private static final class SimpleCommandBuilder implements CommandBuilder {
        private final String literal;
        private final List<String> aliases = new ArrayList<>();
        private final List<ArgumentSpec> arguments = new ArrayList<>();
        private final List<OptionSpec> options = new ArrayList<>();
        private final Map<String, CommandNode> children = new LinkedHashMap<>();
        private CommandExecutor executor = context -> Results.silent();

        private SimpleCommandBuilder(String literal) {
            this.literal = validateLiteral(literal, "literal");
        }

        @Override
        public CommandBuilder alias(String alias) {
            String validatedAlias = validateLiteral(alias, "alias");
            if (validatedAlias.equals(literal) || aliases.contains(validatedAlias)) {
                throw new IllegalArgumentException("alias already registered: " + validatedAlias);
            }
            aliases.add(validatedAlias);
            return this;
        }

        @Override
        public CommandBuilder aliases(String... aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        @Override
        public CommandBuilder subcommand(String literal, Consumer<CommandBuilder> configure) {
            String validatedLiteral = validateLiteral(literal, "literal");
            Objects.requireNonNull(configure, "configure");

            SimpleCommandBuilder builder = new SimpleCommandBuilder(validatedLiteral);
            configure.accept(builder);
            CommandNode child = builder.node();
            registerAll(children, child.literals(), child, "subcommand already registered: ");
            return this;
        }

        @Override
        public <T> CommandBuilder argument(String name, Class<T> type) {
            validateCanAdd(name, ArgumentKind.REQUIRED);
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.REQUIRED));
            return this;
        }

        @Override
        public <T> CommandBuilder optionalArgument(String name, Class<T> type) {
            validateCanAdd(name, ArgumentKind.OPTIONAL);
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.OPTIONAL));
            return this;
        }

        @Override
        public <T> CommandBuilder greedyArgument(String name, Class<T> type) {
            validateCanAdd(name, ArgumentKind.GREEDY);
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.GREEDY));
            return this;
        }

        @Override
        public CommandBuilder flag(String name) {
            return flag(name, null);
        }

        @Override
        public CommandBuilder flag(String name, String alias) {
            options.add(new OptionSpec(name, Boolean.class, alias, OptionKind.FLAG));
            validateOptionNames();
            return this;
        }

        @Override
        public <T> CommandBuilder option(String name, Class<T> type) {
            return option(name, type, null);
        }

        @Override
        public <T> CommandBuilder option(String name, Class<T> type, String alias) {
            options.add(new OptionSpec(name, type, alias, OptionKind.VALUE));
            validateOptionNames();
            return this;
        }

        @Override
        public CommandBuilder executes(CommandExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        CommandNode node() {
            return new CommandNode(literal, aliases, executor, arguments, options, children);
        }

        private void validateCanAdd(String nextName, ArgumentKind nextKind) {
            if (arguments.stream().anyMatch(argument -> argument.name().equals(nextName))) {
                throw new IllegalStateException("argument already declared: " + nextName);
            }

            boolean hasOptional = arguments.stream()
                .anyMatch(argument -> argument.kind() == ArgumentKind.OPTIONAL);
            if (nextKind == ArgumentKind.REQUIRED && hasOptional) {
                throw new IllegalStateException("required arguments must be declared before optional arguments");
            }

            boolean hasGreedy = arguments.stream()
                .anyMatch(argument -> argument.kind() == ArgumentKind.GREEDY);
            if (hasGreedy) {
                throw new IllegalStateException("greedy argument must be the last argument");
            }
        }

        private void validateOptionNames() {
            List<String> seenNames = new ArrayList<>();
            List<String> seenAliases = new ArrayList<>();
            for (OptionSpec option : options) {
                if (arguments.stream().anyMatch(argument -> argument.name().equals(option.name()))) {
                    throw new IllegalStateException("flag or option conflicts with argument: " + option.name());
                }
                if (seenNames.contains(option.name())) {
                    throw new IllegalStateException("flag or option already declared: " + option.name());
                }
                seenNames.add(option.name());
                option.aliasOptional().ifPresent(alias -> {
                    if (seenAliases.contains(alias)) {
                        throw new IllegalStateException("flag or option alias already declared: " + alias);
                    }
                    seenAliases.add(alias);
                });
            }
        }
    }

    private static void registerAll(
        Map<String, CommandNode> nodes,
        List<String> literals,
        CommandNode node,
        String duplicateMessage
    ) {
        for (String literal : literals) {
            if (nodes.containsKey(literal)) {
                throw new IllegalArgumentException(duplicateMessage + literal);
            }
        }
        for (String literal : literals) {
            nodes.put(literal, node);
        }
    }

    private static String validateLiteral(String literal, String label) {
        Objects.requireNonNull(literal, label);
        if (literal.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return literal;
    }

    record CommandNode(
        String literal,
        List<String> aliases,
        CommandExecutor executor,
        List<ArgumentSpec> arguments,
        List<OptionSpec> options,
        Map<String, CommandNode> children
    ) {
        CommandNode {
            Objects.requireNonNull(literal, "literal");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            Objects.requireNonNull(executor, "executor");
            arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
            options = List.copyOf(Objects.requireNonNull(options, "options"));
            children = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(children, "children")));
        }

        List<String> literals() {
            List<String> literals = new ArrayList<>(aliases.size() + 1);
            literals.add(literal);
            literals.addAll(aliases);
            return literals;
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

    record OptionSpec(String name, Class<?> type, String alias, OptionKind kind) {
        OptionSpec {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("flag or option name must not be blank");
            }
            Objects.requireNonNull(type, "type");
            if (alias != null && alias.isBlank()) {
                throw new IllegalArgumentException("flag or option alias must not be blank");
            }
            Objects.requireNonNull(kind, "kind");
        }

        java.util.Optional<String> aliasOptional() {
            return java.util.Optional.ofNullable(alias);
        }
    }

    enum ArgumentKind {
        REQUIRED,
        OPTIONAL,
        GREEDY
    }

    enum OptionKind {
        FLAG,
        VALUE
    }
}
