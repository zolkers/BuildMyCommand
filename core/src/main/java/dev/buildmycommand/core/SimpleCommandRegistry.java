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
    private static final CommandExecutor DEFAULT_EXECUTOR = context -> Results.silent();

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
            configureRoute(builder, 0, route.steps(), executor);
            CommandNode node = builder.node();
            mergeRoot(node);
            return builder;
        }

        private void configureRoute(
            CommandBuilder builder,
            int stepIndex,
            List<RouteStep> steps,
            CommandExecutor executor
        ) {
            try {
                if (stepIndex >= steps.size()) {
                    builder.executes(executor);
                    return;
                }

                RouteStep step = steps.get(stepIndex);
                if (step instanceof LiteralStep literal) {
                    builder.subcommand(literal.value(),
                        child -> configureRoute(child, stepIndex + 1, steps, executor));
                    return;
                }

                ((ElementStep) step).element().apply(builder);
                configureRoute(builder, stepIndex + 1, steps, executor);
            } catch (IllegalStateException exception) {
                throw new IllegalArgumentException("invalid route pattern", exception);
            }
        }
    }

    private static final class SimpleCommandBuilder implements CommandBuilder {
        private final String literal;
        private final List<String> aliases = new ArrayList<>();
        private final List<ArgumentSpec> arguments = new ArrayList<>();
        private final List<OptionSpec> options = new ArrayList<>();
        private final Map<String, CommandNode> children = new LinkedHashMap<>();
        private CommandExecutor executor = DEFAULT_EXECUTOR;

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

    private void mergeRoot(CommandNode node) {
        CommandNode existing = commands.get(node.literal());
        if (existing == null) {
            registerAll(commands, node.literals(), node, "command already registered: ");
            return;
        }

        CommandNode merged = mergeNodes(existing, node);
        replaceNode(commands, existing, merged);
    }

    private static CommandNode mergeNodes(CommandNode existing, CommandNode incoming) {
        if (!existing.literal().equals(incoming.literal())) {
            throw new IllegalArgumentException("cannot merge different literals: " + incoming.literal());
        }

        List<ArgumentSpec> arguments = mergeSpecs(existing.arguments(), incoming.arguments());
        List<OptionSpec> options = mergeSpecs(existing.options(), incoming.options());
        Map<String, CommandNode> children = new LinkedHashMap<>(existing.children());
        for (CommandNode incomingChild : incoming.uniqueChildren()) {
            CommandNode existingChild = children.get(incomingChild.literal());
            if (existingChild == null) {
                registerAll(children, incomingChild.literals(), incomingChild, "subcommand already registered: ");
                continue;
            }

            CommandNode mergedChild = mergeNodes(existingChild, incomingChild);
            replaceNode(children, existingChild, mergedChild);
        }

        if (existing.isExecutable() && incoming.isExecutable()) {
            throw new IllegalArgumentException("command already registered: " + incoming.literal());
        }
        CommandExecutor executor = incoming.isExecutable() ? incoming.executor() : existing.executor();
        return new CommandNode(existing.literal(), existing.aliases(), executor, arguments, options, children);
    }

    private static <T> List<T> mergeSpecs(List<T> existing, List<T> incoming) {
        if (existing.isEmpty()) {
            return incoming;
        }
        if (incoming.isEmpty() || existing.equals(incoming)) {
            return existing;
        }
        throw new IllegalArgumentException("route conflicts with existing command shape");
    }

    private static void replaceNode(Map<String, CommandNode> nodes, CommandNode oldNode, CommandNode newNode) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, CommandNode> entry : nodes.entrySet()) {
            if (entry.getValue() == oldNode) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            nodes.put(key, newNode);
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

        boolean isExecutable() {
            return executor != DEFAULT_EXECUTOR;
        }

        List<CommandNode> uniqueChildren() {
            List<CommandNode> unique = new ArrayList<>();
            for (CommandNode child : children.values()) {
                if (!unique.contains(child)) {
                    unique.add(child);
                }
            }
            return unique;
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
        List<RouteStep> steps
    ) {
        private static ParsedRoute parse(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("route pattern must not be blank");
            }

            String[] tokens = pattern.trim().split("\\s+");
            String rootLiteral = null;
            List<RouteStep> steps = new ArrayList<>();
            boolean seenOption = false;

            for (String token : tokens) {
                RouteElement element = parseElement(token);
                if (element == null) {
                    if (seenOption) {
                        throw new IllegalArgumentException("route options must appear after literals: " + token);
                    }
                    String literal = validateLiteral(token, "route literal");
                    if (rootLiteral == null) {
                        rootLiteral = literal;
                    } else {
                        steps.add(new LiteralStep(literal));
                    }
                    continue;
                }
                if (rootLiteral == null) {
                    throw new IllegalArgumentException("route pattern must start with a literal");
                }
                if (element instanceof OptionRouteElement) {
                    seenOption = true;
                }
                steps.add(new ElementStep(element));
            }

            if (rootLiteral == null) {
                throw new IllegalArgumentException("route pattern must start with a literal");
            }

            return new ParsedRoute(rootLiteral, steps);
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
            Class<?> type = typeFor(typeName);
            if (greedy && type != String.class) {
                throw new IllegalArgumentException("greedy route arguments must be String: " + token);
            }
            return new ArgumentToken(validateName(name, "argument name"), type, greedy);
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
                return new OptionRouteElement(builder -> builder.flag(name, alias));
            }
            if (separator == option.length() - 1 || option.indexOf(':', separator + 1) != -1) {
                throw new IllegalArgumentException("invalid option token: " + token);
            }

            String name = parseLongOptionName(option.substring(0, separator), token);
            Class<?> type = typeFor(option.substring(separator + 1));
            return new OptionRouteElement(builder -> builder.option(name, type, alias));
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

    private record OptionRouteElement(RouteElement delegate) implements RouteElement {
        @Override
        public void apply(CommandBuilder builder) {
            delegate.apply(builder);
        }
    }

    private sealed interface RouteStep permits LiteralStep, ElementStep {
    }

    private record LiteralStep(String value) implements RouteStep {
    }

    private record ElementStep(RouteElement element) implements RouteStep {
    }
}
