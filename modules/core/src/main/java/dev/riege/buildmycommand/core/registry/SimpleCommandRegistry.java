package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandLifecycleListener;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class SimpleCommandRegistry implements CommandRegistry {
    static final CommandExecutor DEFAULT_EXECUTOR = context -> Results.silent();

    private final Map<String, RegistryCommandNode> commands = new LinkedHashMap<>();
    private final CommandMatchingPolicy matchingPolicy;
    private final List<CommandLifecycleListener> lifecycleListeners;

    public SimpleCommandRegistry() {
        this(CommandMatchingPolicy.strict());
    }

    public SimpleCommandRegistry(CommandMatchingPolicy matchingPolicy) {
        this(matchingPolicy, List.of());
    }

    public SimpleCommandRegistry(CommandMatchingPolicy matchingPolicy, List<CommandLifecycleListener> lifecycleListeners) {
        this.matchingPolicy = Objects.requireNonNull(matchingPolicy, "matchingPolicy");
        this.lifecycleListeners = List.copyOf(Objects.requireNonNull(lifecycleListeners, "lifecycleListeners"));
    }

    @Override
    public CommandRegistry caseInsensitiveLiterals() {
        matchingPolicy.enableCaseInsensitiveLiterals();
        return this;
    }

    @Override
    public CommandRegistry caseInsensitiveOptions() {
        matchingPolicy.enableCaseInsensitiveOptions();
        return this;
    }

    @Override
    public void command(String literal, Consumer<CommandBuilder> configure) {
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder(literal, matchingPolicy);
        configure.accept(builder);
        RegistryCommandNode node = builder.node();
        boolean updatesExistingRoot = find(node.literal()) != null;
        RegistryNodeMerger.mergeRoot(commands, node, matchingPolicy);
        RegistryCommandNode eventNode = find(node.literal());
        if (updatesExistingRoot) {
            notifyUpdated(eventNode, List.of(eventNode.literal()));
        } else {
            notifyRegistered(eventNode, List.of(eventNode.literal()));
        }
        notifyRegistryRebuilt();
    }

    @Override
    public void register(CommandNode node) {
        Objects.requireNonNull(node, "node");
        RegistryCommandNode internal = ManualCommandImporter.importNode(node, matchingPolicy);
        RegistryNodeMerger.registerAll(commands, internal.literals(), internal, "command already registered: ", matchingPolicy);
        notifyRegistered(internal, List.of(internal.literal()));
        notifyRegistryRebuilt();
    }

    @Override
    public boolean unregister(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }

        RegistryCommandPath commandPath = findPath(List.of(path.trim().split("\\s+")));
        if (commandPath == null) {
            return false;
        }

        List<RegistryCommandNode> nodes = commandPath.nodes();
        if (nodes.size() == 1) {
            removeRoot(nodes.get(0));
        } else {
            removeNestedPath(nodes);
        }
        notifyUnregistered(commandPath.literals());
        notifyRegistryRebuilt();
        return true;
    }

    @Override
    public RouteBuilder route(String pattern) {
        return new SimpleRouteBuilder(RoutePatternParser.parse(pattern));
    }

    public RegistryCommandNode find(String literal) {
        return commands.get(matchingPolicy.literalKey(literal));
    }

    public RegistryCommandPath findPath(List<String> literals) {
        Objects.requireNonNull(literals, "literals");
        if (literals.isEmpty()) {
            return null;
        }

        RegistryCommandNode command = find(literals.get(0));
        if (command == null) {
            return null;
        }

        List<String> canonicalPath = new ArrayList<>();
        List<RegistryCommandNode> nodes = new ArrayList<>();
        canonicalPath.add(command.literal());
        nodes.add(command);
        for (int index = 1; index < literals.size(); index++) {
            command = command.children().get(matchingPolicy.literalKey(literals.get(index)));
            if (command == null) {
                return null;
            }
            canonicalPath.add(command.literal());
            nodes.add(command);
        }
        return new RegistryCommandPath(canonicalPath, nodes);
    }

    public List<RegistryCommandNode> roots() {
        List<RegistryCommandNode> roots = new ArrayList<>();
        for (RegistryCommandNode command : commands.values()) {
            if (!roots.contains(command)) {
                roots.add(command);
            }
        }
        return roots;
    }

    private void notifyRegistered(RegistryCommandNode command, List<String> path) {
        if (lifecycleListeners.isEmpty()) {
            return;
        }
        CommandNode snapshot = ManualCommandImporter.exportNode(command);
        List<String> immutablePath = List.copyOf(path);
        lifecycleListeners.forEach(listener -> listener.commandRegistered(snapshot, immutablePath));
    }

    private void notifyUpdated(RegistryCommandNode command, List<String> path) {
        if (lifecycleListeners.isEmpty()) {
            return;
        }
        CommandNode snapshot = ManualCommandImporter.exportNode(command);
        List<String> immutablePath = List.copyOf(path);
        lifecycleListeners.forEach(listener -> listener.commandUpdated(snapshot, immutablePath));
    }

    private void notifyRegistryRebuilt() {
        if (lifecycleListeners.isEmpty()) {
            return;
        }
        List<CommandNode> roots = roots().stream()
            .map(ManualCommandImporter::exportNode)
            .toList();
        lifecycleListeners.forEach(listener -> listener.registryRebuilt(roots));
    }

    private void notifyUnregistered(List<String> path) {
        if (lifecycleListeners.isEmpty()) {
            return;
        }
        List<String> immutablePath = List.copyOf(path);
        lifecycleListeners.forEach(listener -> listener.commandUnregistered(immutablePath));
    }

    private RegistryCommandNode findEventNode(List<String> path, RegistryCommandNode fallback) {
        RegistryCommandPath commandPath = findPath(path);
        if (commandPath == null) {
            return fallback;
        }
        return commandPath.nodes().get(commandPath.nodes().size() - 1);
    }

    private void removeNestedPath(List<RegistryCommandNode> nodes) {
        RegistryCommandNode oldChild = nodes.get(nodes.size() - 1);
        RegistryCommandNode newChild = null;

        for (int parentIndex = nodes.size() - 2; parentIndex >= 0; parentIndex--) {
            RegistryCommandNode oldParent = nodes.get(parentIndex);
            Map<String, RegistryCommandNode> children = new LinkedHashMap<>(oldParent.children());
            if (newChild == null) {
                RegistryCommandNode removedChild = oldChild;
                children.entrySet().removeIf(entry -> entry.getValue() == removedChild);
            } else {
                replaceNode(children, oldChild, newChild);
            }

            RegistryCommandNode rebuiltParent = copyWithChildren(oldParent, children);
            oldChild = oldParent;
            if (!rebuiltParent.isExecutable() && rebuiltParent.children().isEmpty()) {
                newChild = null;
                continue;
            }
            newChild = rebuiltParent;
        }

        if (newChild == null) {
            removeRoot(nodes.get(0));
        } else {
            replaceNode(commands, nodes.get(0), newChild);
        }
    }

    private void removeRoot(RegistryCommandNode root) {
        commands.entrySet().removeIf(entry -> entry.getValue() == root);
    }

    private static RegistryCommandNode copyWithChildren(
        RegistryCommandNode node,
        Map<String, RegistryCommandNode> children
    ) {
        return new RegistryCommandNode(
            node.literal(),
            node.description(),
            node.permission(),
            node.aliases(),
            node.executor(),
            node.arguments(),
            node.options(),
            node.metadata(),
            children
        );
    }

    private static void replaceNode(
        Map<String, RegistryCommandNode> nodes,
        RegistryCommandNode oldNode,
        RegistryCommandNode newNode
    ) {
        for (Map.Entry<String, RegistryCommandNode> entry : nodes.entrySet()) {
            if (entry.getValue() == oldNode) {
                entry.setValue(newNode);
            }
        }
    }

    private final class SimpleRouteBuilder implements RouteBuilder {
        private final RoutePattern route;
        private String description;
        private String permission;
        private final CommandMetadata.Builder metadata = new CommandMetadata.Builder();
        private final Map<String, SuggestionProvider> argumentSuggestions = new LinkedHashMap<>();
        private final Map<String, SuggestionProvider> optionSuggestions = new LinkedHashMap<>();

        private SimpleRouteBuilder(RoutePattern route) {
            this.route = route;
        }

        @Override
        public RouteBuilder description(String description) {
            this.description = Validators.metadata(description, "description");
            return this;
        }

        @Override
        public RouteBuilder permission(String permission) {
            this.permission = Validators.metadata(permission, "permission");
            return this;
        }

        @Override
        public RouteBuilder hidden() {
            metadata.hidden();
            return this;
        }

        @Override
        public RouteBuilder usage(String usage) {
            metadata.usage(usage);
            return this;
        }

        @Override
        public RouteBuilder example(String example) {
            metadata.example(example);
            return this;
        }

        @Override
        public RouteBuilder cooldown(Duration cooldown) {
            metadata.cooldown(cooldown);
            return this;
        }

        @Override
        public RouteBuilder requirement(String requirement) {
            metadata.requirement(requirement);
            return this;
        }

        @Override
        public RouteBuilder group(String group) {
            metadata.group(group);
            return this;
        }

        @Override
        public RouteBuilder suggestAliases(boolean suggestAliases) {
            metadata.suggestAliases(suggestAliases);
            return this;
        }

        @Override
        public RouteBuilder middleware(CommandMiddleware middleware) {
            metadata.middleware(middleware);
            return this;
        }

        @Override
        public RouteBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            return argumentSuggestions(name, null, provider);
        }

        @Override
        public RouteBuilder argumentSuggestions(String name, String providerName, SuggestionProvider provider) {
            argumentSuggestions.put(
                Objects.requireNonNull(name, "name"),
                namedProvider(providerName, Objects.requireNonNull(provider, "provider"))
            );
            return this;
        }

        @Override
        public RouteBuilder optionSuggestions(String name, SuggestionProvider provider) {
            return optionSuggestions(name, null, provider);
        }

        @Override
        public RouteBuilder optionSuggestions(String name, String providerName, SuggestionProvider provider) {
            optionSuggestions.put(
                Objects.requireNonNull(name, "name"),
                namedProvider(providerName, Objects.requireNonNull(provider, "provider"))
            );
            return this;
        }

        @Override
        public CommandBuilder executes(CommandExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            SimpleCommandBuilder builder = new SimpleCommandBuilder(route.rootLiteral(), matchingPolicy);
            builder.aliases(route.rootAliases().toArray(String[]::new));
            configureRoute(builder, 0, route.steps(), executor);
            List<String> path = commandPath(route);
            boolean updatesExistingPath = findPath(path) != null;
            RegistryNodeMerger.mergeRoot(commands, builder.node(), matchingPolicy);
            RegistryCommandNode eventNode = findEventNode(path, builder.node());
            if (updatesExistingPath) {
                notifyUpdated(eventNode, path);
            } else {
                notifyRegistered(eventNode, path);
            }
            notifyRegistryRebuilt();
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
                    applyMetadata(builder);
                    builder.executes(executor);
                    return;
                }

                RouteStep step = steps.get(stepIndex);
                if (step instanceof LiteralRouteStep literal) {
                    builder.subcommand(literal.value(),
                        child -> {
                            child.aliases(literal.aliases().toArray(String[]::new));
                            configureRoute(child, stepIndex + 1, steps, executor);
                        });
                    return;
                }

                ((ElementRouteStep) step).element().apply(builder);
                configureRoute(builder, stepIndex + 1, steps, executor);
            } catch (IllegalStateException exception) {
                throw new IllegalArgumentException("invalid route pattern", exception);
            }
        }

        private void applyMetadata(CommandBuilder builder) {
            if (description != null) {
                builder.description(description);
            }
            if (permission != null) {
                builder.permission(permission);
            }
            CommandMetadata builtMetadata = metadata.build();
            if (builtMetadata.hidden()) {
                builder.hidden();
            }
            builtMetadata.usage().ifPresent(builder::usage);
            builtMetadata.examples().forEach(builder::example);
            builtMetadata.cooldown().ifPresent(builder::cooldown);
            builtMetadata.requirement().ifPresent(builder::requirement);
            builtMetadata.group().ifPresent(builder::group);
            builder.suggestAliases(builtMetadata.suggestAliases());
            builtMetadata.middlewares().forEach(builder::middleware);
            argumentSuggestions.forEach((name, provider) ->
                builder.argumentSuggestions(name, providerName(provider), provider));
            optionSuggestions.forEach((name, provider) ->
                builder.optionSuggestions(name, providerName(provider), provider));
        }

        private static SuggestionProvider namedProvider(String name, SuggestionProvider provider) {
            return name == null ? provider : new NamedSuggestionProvider(name, provider);
        }

        private static String providerName(SuggestionProvider provider) {
            return provider instanceof NamedSuggestionProvider named ? named.name() : null;
        }

        private static List<String> commandPath(RoutePattern route) {
            List<String> path = new ArrayList<>();
            path.add(route.rootLiteral());
            for (RouteStep step : route.steps()) {
                if (step instanceof LiteralRouteStep literal) {
                    path.add(literal.value());
                }
            }
            return List.copyOf(path);
        }
    }

    private record NamedSuggestionProvider(String name, SuggestionProvider delegate) implements SuggestionProvider {
        @Override
        public List<String> suggestions(ArgumentParseContext context) {
            return delegate.suggestions(context);
        }

        @Override
        public List<Suggestion> richSuggestions(
            ArgumentParseContext context
        ) {
            return delegate.richSuggestions(context);
        }
    }
}
