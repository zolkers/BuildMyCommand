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

    @Override
    public RouteBuilder route(String pattern) {
        ParsedRoute route = ParsedRoute.parse(pattern);
        return new SimpleRouteBuilder(route);
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

    private final class SimpleRouteBuilder implements RouteBuilder {
        private final ParsedRoute route;

        private SimpleRouteBuilder(ParsedRoute route) {
            this.route = route;
        }

        @Override
        public CommandBuilder executes(CommandExecutor executor) {
            Objects.requireNonNull(executor, "executor");
            SimpleCommandBuilder builder = new SimpleCommandBuilder(route.rootLiteral());
            configureRoute(builder, 0, route, executor);
            CommandNode node = builder.node();
            registerAll(commands, node.literals(), node, "command already registered: ");
            return builder;
        }

        private void configureRoute(
            CommandBuilder builder,
            int childLiteralIndex,
            ParsedRoute route,
            CommandExecutor executor
        ) {
            if (childLiteralIndex < route.childLiterals().size()) {
                builder.subcommand(route.childLiterals().get(childLiteralIndex),
                    child -> configureRoute(child, childLiteralIndex + 1, route, executor));
                return;
            }

            for (RouteElement element : route.elements()) {
                element.apply(builder);
            }
            builder.executes(executor);
        }
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

        private CommandBuilder optionalGreedyArgument(String name, Class<?> type) {
            validateCanAdd(name, ArgumentKind.OPTIONAL_GREEDY);
            arguments.add(new ArgumentSpec(name, type, ArgumentKind.OPTIONAL_GREEDY));
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
            if (options.stream().anyMatch(option -> option.name().equals(nextName))) {
                throw new IllegalStateException("argument conflicts with flag or option: " + nextName);
            }

            boolean hasOptional = arguments.stream()
                .anyMatch(argument -> argument.kind() == ArgumentKind.OPTIONAL
                    || argument.kind() == ArgumentKind.OPTIONAL_GREEDY);
            if (nextKind == ArgumentKind.REQUIRED && hasOptional) {
                throw new IllegalStateException("required arguments must be declared before optional arguments");
            }

            boolean hasGreedy = arguments.stream()
                .anyMatch(argument -> argument.kind() == ArgumentKind.GREEDY
                    || argument.kind() == ArgumentKind.OPTIONAL_GREEDY);
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
        GREEDY,
        OPTIONAL_GREEDY
    }

    enum OptionKind {
        FLAG,
        VALUE
    }

    private record ParsedRoute(
        String rootLiteral,
        List<String> childLiterals,
        List<RouteElement> elements
    ) {
        private static ParsedRoute parse(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("route pattern must not be blank");
            }

            String[] tokens = pattern.trim().split("\\s+");
            List<String> literals = new ArrayList<>();
            List<RouteElement> elements = new ArrayList<>();
            boolean seenNonLiteral = false;

            for (String token : tokens) {
                RouteElement element = parseElement(token);
                if (element == null) {
                    if (seenNonLiteral) {
                        throw new IllegalArgumentException("route literals must appear before arguments and options: " + token);
                    }
                    literals.add(validateLiteral(token, "route literal"));
                    continue;
                }
                seenNonLiteral = true;
                elements.add(element);
            }

            if (literals.isEmpty()) {
                throw new IllegalArgumentException("route pattern must start with a literal");
            }

            return new ParsedRoute(literals.get(0), literals.subList(1, literals.size()), elements);
        }

        private static RouteElement parseElement(String token) {
            if (token.startsWith("<") || token.endsWith(">")) {
                if (!token.startsWith("<") || !token.endsWith(">")) {
                    throw new IllegalArgumentException("invalid required argument token: " + token);
                }
                ArgumentToken argument = parseArgumentToken(token.substring(1, token.length() - 1), token);
                if (argument.greedy()) {
                    return builder -> builder.greedyArgument(argument.name(), argument.type());
                }
                return builder -> builder.argument(argument.name(), argument.type());
            }

            if (token.startsWith("[") || token.endsWith("]")) {
                if (!token.startsWith("[") || !token.endsWith("]")) {
                    throw new IllegalArgumentException("invalid optional token: " + token);
                }
                String body = token.substring(1, token.length() - 1);
                if (body.startsWith("--")) {
                    return parseOptionToken(body, token);
                }

                ArgumentToken argument = parseArgumentToken(body, token);
                if (argument.greedy()) {
                    return builder -> ((SimpleCommandBuilder) builder).optionalGreedyArgument(argument.name(), argument.type());
                }
                return builder -> builder.optionalArgument(argument.name(), argument.type());
            }

            return null;
        }

        private static ArgumentToken parseArgumentToken(String body, String token) {
            int separator = body.indexOf(':');
            if (separator <= 0 || separator == body.length() - 1 || body.indexOf(':', separator + 1) != -1) {
                throw new IllegalArgumentException("invalid argument token: " + token);
            }

            String name = body.substring(0, separator);
            String typeName = body.substring(separator + 1);
            boolean greedy = typeName.endsWith("...");
            if (greedy) {
                typeName = typeName.substring(0, typeName.length() - 3);
            }
            return new ArgumentToken(validateName(name, "argument name"), typeFor(typeName), greedy);
        }

        private static RouteElement parseOptionToken(String body, String token) {
            String[] parts = body.split("\\|", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException("invalid option token: " + token);
            }

            String option = parts[0];
            String alias = parts.length == 2 ? parseAlias(parts[1], token) : null;
            int separator = option.indexOf(':');
            if (separator < 0) {
                String name = parseLongOptionName(option, token);
                return builder -> builder.flag(name, alias);
            }
            if (separator == option.length() - 1 || option.indexOf(':', separator + 1) != -1) {
                throw new IllegalArgumentException("invalid option token: " + token);
            }

            String name = parseLongOptionName(option.substring(0, separator), token);
            Class<?> type = typeFor(option.substring(separator + 1));
            return builder -> builder.option(name, type, alias);
        }

        private static String parseLongOptionName(String raw, String token) {
            if (!raw.startsWith("--") || raw.length() <= 2) {
                throw new IllegalArgumentException("invalid long option token: " + token);
            }
            return validateName(raw.substring(2), "option name");
        }

        private static String parseAlias(String raw, String token) {
            if (!raw.startsWith("-") || raw.startsWith("--") || raw.length() != 2) {
                throw new IllegalArgumentException("invalid option alias token: " + token);
            }
            return validateName(raw.substring(1), "option alias");
        }

        private static Class<?> typeFor(String typeName) {
            return switch (typeName) {
                case "String" -> String.class;
                case "Integer" -> Integer.class;
                case "int" -> int.class;
                default -> throw new IllegalArgumentException("unknown route type: " + typeName);
            };
        }

        private static String validateName(String name, String label) {
            Objects.requireNonNull(name, label);
            if (name.isBlank()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            for (int index = 0; index < name.length(); index++) {
                char character = name.charAt(index);
                if (!Character.isLetterOrDigit(character) && character != '-' && character != '_') {
                    throw new IllegalArgumentException("invalid " + label + ": " + name);
                }
            }
            return name;
        }
    }

    private record ArgumentToken(String name, Class<?> type, boolean greedy) {
    }

    @FunctionalInterface
    private interface RouteElement {
        void apply(CommandBuilder builder);
    }
}
