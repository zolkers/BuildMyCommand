package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.Results;
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

    public SimpleCommandRegistry() {
        this(CommandMatchingPolicy.strict());
    }

    public SimpleCommandRegistry(CommandMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(matchingPolicy, "matchingPolicy");
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
        private final dev.riege.buildmycommand.api.CommandMetadata.Builder metadata =
            new dev.riege.buildmycommand.api.CommandMetadata.Builder();
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
            dev.riege.buildmycommand.api.CommandMetadata builtMetadata = metadata.build();
            if (builtMetadata.hidden()) {
                builder.hidden();
            }
            builtMetadata.usage().ifPresent(builder::usage);
            builtMetadata.examples().forEach(builder::example);
            builtMetadata.cooldown().ifPresent(builder::cooldown);
            builtMetadata.requirement().ifPresent(builder::requirement);
            builtMetadata.group().ifPresent(builder::group);
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
    }

    private record NamedSuggestionProvider(String name, SuggestionProvider delegate) implements SuggestionProvider {
        @Override
        public java.util.List<String> suggestions(dev.riege.buildmycommand.api.ArgumentParseContext context) {
            return delegate.suggestions(context);
        }

        @Override
        public java.util.List<dev.riege.buildmycommand.api.Suggestion> richSuggestions(
            dev.riege.buildmycommand.api.ArgumentParseContext context
        ) {
            return delegate.richSuggestions(context);
        }
    }
}
