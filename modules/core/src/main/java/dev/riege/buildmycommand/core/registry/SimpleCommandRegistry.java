package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.Results;

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

    public SimpleCommandRegistry() {
        this(CommandMatchingPolicy.strict());
    }

    public SimpleCommandRegistry(CommandMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(matchingPolicy, "matchingPolicy");
    }

    @Override
    public void command(String literal, Consumer<CommandBuilder> configure) {
        Objects.requireNonNull(configure, "configure");

        SimpleCommandBuilder builder = new SimpleCommandBuilder(literal, matchingPolicy);
        configure.accept(builder);
        RegistryCommandNode node = builder.node();
        RegistryNodeMerger.registerAll(commands, node.literals(), node, "command already registered: ", matchingPolicy);
    }

    @Override
    public void register(CommandNode node) {
        Objects.requireNonNull(node, "node");
        RegistryCommandNode internal = ManualCommandImporter.importNode(node, matchingPolicy);
        RegistryNodeMerger.registerAll(commands, internal.literals(), internal, "command already registered: ", matchingPolicy);
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

    private final class SimpleRouteBuilder implements RouteBuilder {
        private final RoutePattern route;
        private String description;
        private String permission;

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
        public CommandBuilder executes(CommandExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            SimpleCommandBuilder builder = new SimpleCommandBuilder(route.rootLiteral(), matchingPolicy);
            builder.aliases(route.rootAliases().toArray(String[]::new));
            configureRoute(builder, 0, route.steps(), executor);
            RegistryNodeMerger.mergeRoot(commands, builder.node(), matchingPolicy);
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
        }
    }
}
