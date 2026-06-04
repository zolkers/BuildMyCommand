/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core;


import dev.riege.buildmycommand.core.dispatch.CommandDispatcher;
import dev.riege.buildmycommand.core.help.HelpGenerator;
import dev.riege.buildmycommand.core.help.SchemaExporter;
import dev.riege.buildmycommand.core.help.SuggestionEngine;
import dev.riege.buildmycommand.core.middleware.CooldownMiddleware;
import dev.riege.buildmycommand.core.middleware.MiddlewareChain;
import dev.riege.buildmycommand.core.parse.ArgumentParserRegistry;
import dev.riege.buildmycommand.core.parse.ArgumentResolver;
import dev.riege.buildmycommand.core.parse.CommandTokenizer;
import dev.riege.buildmycommand.core.parse.OptionParser;
import dev.riege.buildmycommand.core.registry.ManualCommandImporter;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import dev.riege.buildmycommand.api.ArgumentParser;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandErrorHandler;
import dev.riege.buildmycommand.api.CommandExceptionContext;
import dev.riege.buildmycommand.api.CommandExceptionHandler;
import dev.riege.buildmycommand.api.CommandExceptionHandlers;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandLifecycleListener;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.CommandTypeRegistry;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionProvider;
import dev.riege.buildmycommand.dsl.RouteParser;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;
    private final CommandDispatcher dispatcher;
    private final SuggestionEngine suggestions;
    private final HelpGenerator help;
    private final SchemaExporter schema;
    private final CommandMatchingPolicy matchingPolicy;

    private CommandFramework(
        SimpleCommandRegistry registry,
        CommandMatchingPolicy matchingPolicy,
        ArgumentParserRegistry parsers,
        List<CommandMiddleware> middleware,
        CommandExceptionHandler exceptionHandler,
        Clock cooldownClock,
        ConcurrentMap<CooldownMiddleware.Key, Instant> cooldownStore
    ) {
        CommandTokenizer tokenizer = new CommandTokenizer();
        ArgumentResolver argumentResolver = new ArgumentResolver(parsers);
        OptionParser optionParser = new OptionParser(parsers, matchingPolicy);
        List<CommandMiddleware> executionMiddleware = new ArrayList<>(middleware);
        executionMiddleware.add(new CooldownMiddleware(cooldownClock, cooldownStore));

        this.registry = registry;
        this.matchingPolicy = matchingPolicy;
        this.dispatcher = new CommandDispatcher(
            registry,
            tokenizer,
            optionParser,
            argumentResolver,
            matchingPolicy,
            new MiddlewareChain(executionMiddleware),
            exceptionHandler
        );
        this.suggestions = new SuggestionEngine(registry, tokenizer, matchingPolicy, parsers);
        this.help = new HelpGenerator(registry, tokenizer);
        this.schema = new SchemaExporter();
    }

    public static CommandFramework create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public CommandRegistry registry() {
        return registry;
    }

    public List<String> rootLiterals() {
        return registry.roots().stream()
            .map(command -> command.literal())
            .toList();
    }

    public List<String> rootLabels() {
        return registry.roots().stream()
            .flatMap(command -> {
                List<String> labels = new ArrayList<>();
                labels.add(command.literal());
                labels.addAll(command.aliases());
                return labels.stream();
            })
            .distinct()
            .toList();
    }

    public boolean caseInsensitiveLiterals() {
        return matchingPolicy.caseInsensitiveLiterals();
    }

    public boolean caseInsensitiveOptions() {
        return matchingPolicy.caseInsensitiveOptions();
    }

    public String help(String path) {
        return help(new CommandSource() {
        }, path);
    }

    public String help(CommandSource source, String path) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(path, "path");
        return help.help(source, path);
    }

    public String schema() {
        return schema.schema(registry);
    }

    public CommandGraph graph() {
        return new CommandGraph(registry.roots().stream()
            .map(ManualCommandImporter::exportNode)
            .toList());
    }

    public CommandResult dispatch(CommandInput input) {
        Objects.requireNonNull(input, "input");
        return dispatcher.dispatch(input);
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return dispatcher.dispatch(source, input);
    }

    public List<Suggestion> suggestRich(CommandInput input) {
        Objects.requireNonNull(input, "input");
        return suggestions.suggestRich(input);
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return suggestions.suggest(source, input, cursor);
    }

    public static final class Builder {
        private boolean caseInsensitiveLiterals;
        private boolean caseInsensitiveOptions;
        private final ArgumentParserRegistry parsers = new ArgumentParserRegistry();
        private final Map<String, Class<?>> routeTypes = new LinkedHashMap<>();
        private final List<CommandMiddleware> middleware = new ArrayList<>();
        private final List<CommandLifecycleListener> lifecycleListeners = new ArrayList<>();
        private CommandExceptionHandler exceptionHandler = Builder::defaultHandler;
        private Clock cooldownClock = Clock.systemUTC();
        private ConcurrentMap<CooldownMiddleware.Key, Instant> cooldownStore = new ConcurrentHashMap<>();

        private Builder() {
        }

        public Builder caseInsensitiveLiterals() {
            caseInsensitiveLiterals = true;
            return this;
        }

        public Builder caseInsensitiveOptions() {
            caseInsensitiveOptions = true;
            return this;
        }

        public <T> Builder argumentParser(Class<T> type, ArgumentParser<? extends T> parser) {
            parsers.register(type, parser);
            return this;
        }

        /**
         * Registers one or more user-friendly route DSL type aliases.
         *
         * <p>Use this when commands should say {@code <item:Material>} instead of exposing an internal Java class
         * name or falling back to {@code String}. Each alias also registers the parser that turns the raw token into
         * the Java value returned by {@link CommandContext#arg(String, Class)} and
         * {@link CommandContext#option(String, Class)}.</p>
         *
         * <pre>{@code
         * CommandFramework framework = CommandFramework.builder()
         *     .types(types -> types.register("Material", Material.class, new MaterialParser()))
         *     .build();
         * }</pre>
         *
         * @param configure callback that receives the mutable command type registry
         * @return this builder
         */
        public Builder types(Consumer<CommandTypeRegistry> configure) {
            Objects.requireNonNull(configure, "configure").accept(new BuilderCommandTypeRegistry());
            return this;
        }

        /**
         * Registers a single user-friendly route DSL type alias and its parser.
         *
         * <pre>{@code
         * CommandFramework framework = CommandFramework.builder()
         *     .type("Material", Material.class, new MaterialParser())
         *     .build();
         * }</pre>
         *
         * @param alias DSL type name, such as {@code Material}
         * @param type Java type produced by the parser
         * @param parser parser used for arguments and options with this type
         * @param <T> parsed Java type
         * @return this builder
         * @throws IllegalArgumentException when the alias or Java type is already registered
         */
        public <T> Builder type(String alias, Class<T> type, ArgumentParser<? extends T> parser) {
            registerRouteType(alias, type);
            parsers.register(type, parser);
            return this;
        }

        public <T> Builder suggestionProvider(Class<T> type, SuggestionProvider provider) {
            parsers.registerSuggestions(type, provider);
            return this;
        }

        public Builder middleware(CommandMiddleware middleware) {
            this.middleware.add(Objects.requireNonNull(middleware, "middleware"));
            return this;
        }

        public Builder errorHandler(CommandErrorHandler errorHandler) {
            Objects.requireNonNull(errorHandler, "errorHandler");
            this.exceptionHandler = (exceptionContext, error) -> {
                if (exceptionContext.context().isEmpty() || exceptionContext.command().isEmpty()) {
                    return CommandExceptionHandlers.failureMessage().handle(exceptionContext, error);
                }
                return errorHandler.handle(
                    exceptionContext.context().get(),
                    exceptionContext.command().get(),
                    exceptionContext.commandPath(),
                    error
                );
            };
            return this;
        }

        public Builder exceptionHandler(CommandExceptionHandler exceptionHandler) {
            this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
            return this;
        }

        public Builder lifecycleListener(CommandLifecycleListener lifecycleListener) {
            lifecycleListeners.add(Objects.requireNonNull(lifecycleListener, "lifecycleListener"));
            return this;
        }

        public Builder cooldownClock(Clock cooldownClock) {
            this.cooldownClock = Objects.requireNonNull(cooldownClock, "cooldownClock");
            return this;
        }

        public Builder cooldownStore(ConcurrentMap<CooldownMiddleware.Key, Instant> cooldownStore) {
            this.cooldownStore = Objects.requireNonNull(cooldownStore, "cooldownStore");
            return this;
        }

        public CommandFramework build() {
            CommandMatchingPolicy matchingPolicy =
                new CommandMatchingPolicy(caseInsensitiveLiterals, caseInsensitiveOptions);
            return new CommandFramework(
                new SimpleCommandRegistry(matchingPolicy, lifecycleListeners, routeTypes),
                matchingPolicy,
                new ArgumentParserRegistry(parsers.parsers(), parsers.suggestionProviders()),
                middleware,
                exceptionHandler,
                cooldownClock,
                cooldownStore
            );
        }

        private static CommandResult rethrow(
            CommandContext context,
            CommandNode command,
            List<String> path,
            Throwable error
        ) {
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (error instanceof Error fatal) {
                throw fatal;
            }
            throw new RuntimeException(error);
        }

        private static CommandResult defaultHandler(CommandExceptionContext context, Throwable error) {
            return CommandExceptionHandlers.failureMessage().handle(context, error);
        }

        private <T> void registerRouteType(String alias, Class<T> type) {
            String validatedAlias = validateTypeAlias(alias);
            Objects.requireNonNull(type, "type");
            Map<String, Class<?>> allTypes = new LinkedHashMap<>(RouteParser.defaultTypes());
            allTypes.putAll(routeTypes);
            if (allTypes.containsKey(validatedAlias)) {
                throw new IllegalArgumentException("command type alias already registered: " + validatedAlias);
            }
            allTypes.entrySet().stream()
                .filter(entry -> entry.getValue().equals(type))
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalArgumentException("command type already registered as "
                        + entry.getKey() + ": " + type.getName());
                });
            routeTypes.put(validatedAlias, type);
        }

        private static String validateTypeAlias(String alias) {
            Objects.requireNonNull(alias, "alias");
            if (!alias.matches("[A-Za-z][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("invalid command type alias: " + alias);
            }
            return alias;
        }

        private final class BuilderCommandTypeRegistry implements CommandTypeRegistry {
            @Override
            public <T> CommandTypeRegistry register(
                String alias,
                Class<T> type,
                ArgumentParser<? extends T> parser
            ) {
                Builder.this.type(alias, type, parser);
                return this;
            }
        }
    }
}
