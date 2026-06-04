/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.annotation.SuggestAliases;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.PermissionSpec;
import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteParser;
import dev.riege.buildmycommand.dsl.RouteStep;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AnnotationCommandCompiler {
    private AnnotationCommandCompiler() {
    }

    public static CompiledCommands compile(Object commands) {
        return compile(commands, Map.of());
    }

    public static CompiledCommands compile(Object commands, Map<String, Class<?>> routeTypes) {
        Objects.requireNonNull(commands, "commands");
        routeTypes = Map.copyOf(Objects.requireNonNull(routeTypes, "routeTypes"));
        Class<?> owner = commands.getClass();
        CasePolicy classCasePolicy = casePolicy(owner.getAnnotation(CaseInsensitive.class));
        Optional<String> group = commandGroup(owner);
        Optional<RootCommand> rootCommand = rootCommand(owner, classCasePolicy);

        Alias rootAlias = owner.getAnnotation(Alias.class);
        List<String> rootAliases = rootAlias == null ? List.of() : List.of(rootAlias.value());
        List<CompiledCommand> compiled = new ArrayList<>();
        collectCommands(commands, owner, owner.getAnnotation(Command.class), rootAliases, group, List.of(), compiled,
            routeTypes);
        validateSuggestionProviders(compiled, routeTypes);

        return new CompiledCommands(classCasePolicy, rootCommand, compiled);
    }

    private static void collectCommands(Object target, Class<?> owner, Command rootCommand, List<String> rootAliases,
                                        Optional<String> group, List<SubcommandSegment> prefix,
                                        List<CompiledCommand> compiled, Map<String, Class<?>> routeTypes) {
        Arrays.stream(owner.getDeclaredMethods())
            .filter(method -> isAnnotatedCommandMethod(rootCommand, prefix, method))
            .sorted(Comparator
                .comparing((Method method) -> routeKey(rootCommand, prefix, method))
                .thenComparing(AnnotationCommandCompiler::signature))
            .map(method -> compileMethod(target, method, rootCommand, rootAliases, prefix, group, routeTypes))
            .forEach(compiled::add);

        Arrays.stream(owner.getDeclaredClasses())
            .filter(nested -> nested.isAnnotationPresent(Subcommand.class))
            .sorted(Comparator.comparing(Class::getName))
            .forEach(nested -> {
                SubcommandSegment segment = subcommandSegment(nested);
                Object nestedTarget = instantiateNestedCommandGroup(target, nested);
                List<SubcommandSegment> nestedPrefix = new ArrayList<>(prefix);
                nestedPrefix.add(segment);
                collectCommands(nestedTarget, nested, rootCommand, rootAliases, group, nestedPrefix, compiled, routeTypes);
            });
    }

    private static CompiledCommand compileMethod(
        Object target,
        Method method,
        Command ownerCommand,
        List<String> rootAliases,
        List<SubcommandSegment> prefix,
        Optional<String> group,
        Map<String, Class<?>> routeTypes
    ) {
        Command command = method.getAnnotation(Command.class);
        Route route = method.getAnnotation(Route.class);
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        SubRoute subRoute = method.getAnnotation(SubRoute.class);
        if (annotationCount(command, route, subcommand, subRoute) > 1) {
            throw new IllegalArgumentException("annotated command method cannot mix command route annotations: "
                + method.getName());
        }

        MethodCommandBinder.BoundMethod boundMethod = MethodCommandBinder.bind(target, method);
        Optional<String> description = metadata(method.getAnnotation(Description.class), "description");
        Optional<PermissionSpec> permission = permission(method.getAnnotation(Permission.class));
        CasePolicy methodCasePolicy = casePolicy(method.getAnnotation(CaseInsensitive.class));
        Optional<String> effectiveGroup = commandGroup(method).or(() -> group);

        if (ownerCommand != null) {
            validateSingleLiteral(ownerCommand.value(), "@Command", "@Route");
        }

        if (subRoute != null) {
            String commandRoute = AnnotationRouteAliases.aliasedSubcommandRoute(
                ownerCommand.value() + prefixedRoute(prefix, subRoute.value()),
                rootAliases,
                method.getAnnotation(Alias.class),
                prefix.size() + 1
            );
            AnnotationRouteValidator.validateRouteContextUsage(commandRoute, method, boundMethod.bindings(),
                routeTypes);
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                effectiveGroup,
                List.of(),
                List.of(),
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }
        if (subcommand != null) {
            validateSingleLiteral(subcommand.value(), "@Subcommand", "@SubRoute");
            Alias methodAlias = method.getAnnotation(Alias.class);
            return new CompiledCommand(
                RegistrationKind.SUBCOMMAND,
                ownerCommand.value(),
                effectiveGroup,
                rootAliases,
                withLeaf(prefix, subcommand.value(), methodAlias),
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }
        if (route != null) {
            String commandRoute = AnnotationRouteAliases.aliasedRoute(route.value(), method.getAnnotation(Alias.class));
            AnnotationRouteValidator.validateRouteContextUsage(commandRoute, method, boundMethod.bindings(),
                routeTypes);
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                effectiveGroup,
                List.of(),
                List.of(),
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }

        Alias alias = method.getAnnotation(Alias.class);
        validateSingleLiteral(command.value(), "@Command", "@Route");
        return new CompiledCommand(
            RegistrationKind.COMMAND,
            command.value(),
            effectiveGroup,
            alias == null ? List.of() : List.of(alias.value()),
            List.of(),
            List.of(),
            description,
            permission,
            methodCasePolicy,
            boundMethod
        );
    }

    private static boolean isAnnotatedCommandMethod(Command rootCommand, List<SubcommandSegment> prefix, Method method) {
        boolean hasRoot = rootCommand != null;
        boolean nested = !prefix.isEmpty();
        return (!nested && (method.isAnnotationPresent(Command.class) || method.isAnnotationPresent(Route.class)))
            || (hasRoot && (method.isAnnotationPresent(Subcommand.class) || method.isAnnotationPresent(SubRoute.class)));
    }

    private static String routeKey(Command rootCommand, List<SubcommandSegment> prefix, Method method) {
        Command command = method.getAnnotation(Command.class);
        if (command != null) {
            return command.value();
        }
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        if (subcommand != null) {
            return rootCommand.value() + prefixedRoute(prefix, subcommand.value());
        }
        SubRoute subRoute = method.getAnnotation(SubRoute.class);
        if (subRoute != null) {
            return rootCommand.value() + prefixedRoute(prefix, subRoute.value());
        }
        return method.getAnnotation(Route.class).value();
    }

    private static List<SubcommandSegment> withLeaf(
        List<SubcommandSegment> prefix,
        String literal,
        Alias alias
    ) {
        validateSingleLiteral(literal, "@Subcommand", "@SubRoute");
        List<SubcommandSegment> path = new ArrayList<>(prefix);
        path.add(new SubcommandSegment(
            literal,
            alias == null ? List.of() : List.of(alias.value()),
            Optional.empty(),
            Optional.empty(),
            emptyMetadata()
        ));
        return path;
    }

    private static String prefixedRoute(List<SubcommandSegment> prefix, String route) {
        if (prefix.isEmpty()) {
            return " " + route;
        }
        List<String> tokens = new ArrayList<>();
        for (SubcommandSegment segment : prefix) {
            tokens.add(segment.routeToken());
        }
        tokens.add(route);
        return " " + String.join(" ", tokens);
    }

    private static SubcommandSegment subcommandSegment(Class<?> owner) {
        Subcommand subcommand = owner.getAnnotation(Subcommand.class);
        validateSingleLiteral(subcommand.value(), "@Subcommand", "@SubRoute");
        Alias alias = owner.getAnnotation(Alias.class);
        return new SubcommandSegment(
            subcommand.value(),
            alias == null ? List.of() : List.of(alias.value()),
            metadata(owner.getAnnotation(Description.class), "description"),
            permission(owner.getAnnotation(Permission.class)),
            classMetadata(owner)
        );
    }

    private static Object instantiateNestedCommandGroup(Object parent, Class<?> nested) {
        try {
            Constructor<?> constructor;
            Object[] arguments;
            if (nested.isMemberClass() && !Modifier.isStatic(nested.getModifiers())) {
                constructor = nested.getDeclaredConstructor(parent.getClass());
                arguments = new Object[] {parent};
            } else {
                constructor = nested.getDeclaredConstructor();
                arguments = new Object[0];
            }
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("cannot instantiate nested @Subcommand group: "
                + nested.getName(), exception);
        }
    }

    private static MethodCommandBinder.CommandMetadata emptyMetadata() {
        return new MethodCommandBinder.CommandMetadata(
            false,
            Optional.empty(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            true,
            List.of(),
            List.of()
        );
    }

    private static Optional<String> commandGroup(AnnotatedElement owner) {
        CommandGroup group = owner.getAnnotation(CommandGroup.class);
        return group == null ? Optional.empty() : Optional.of(validateMetadata(group.value(), "command group"));
    }

    private static Optional<RootCommand> rootCommand(Class<?> owner, CasePolicy casePolicy) {
        Command command = owner.getAnnotation(Command.class);
        if (command == null || !hasRootMetadata(owner)) {
            return Optional.empty();
        }
        validateSingleLiteral(command.value(), "@Command", "@Route");
        Alias alias = owner.getAnnotation(Alias.class);
        return Optional.of(new RootCommand(
            command.value(),
            alias == null ? List.of() : List.of(alias.value()),
            Optional.empty(),
            metadata(owner.getAnnotation(Description.class), "description"),
            permission(owner.getAnnotation(Permission.class)),
            classMetadata(owner),
            casePolicy
        ));
    }

    private static boolean hasRootMetadata(Class<?> owner) {
        return owner.isAnnotationPresent(Description.class)
            || owner.isAnnotationPresent(Permission.class)
            || owner.isAnnotationPresent(Hidden.class)
            || owner.isAnnotationPresent(Usage.class)
            || owner.isAnnotationPresent(Example.class)
            || owner.isAnnotationPresent(Cooldown.class)
            || owner.isAnnotationPresent(Require.class)
            || owner.isAnnotationPresent(SuggestAliases.class)
            || owner.isAnnotationPresent(Middleware.class);
    }

    private static MethodCommandBinder.CommandMetadata classMetadata(Class<?> owner) {
        Usage usage = owner.getAnnotation(Usage.class);
        Example example = owner.getAnnotation(Example.class);
        Cooldown cooldown = owner.getAnnotation(Cooldown.class);
        Require requirement = owner.getAnnotation(Require.class);

        List<String> examples = new ArrayList<>();
        if (example != null) {
            for (String value : example.value()) {
                examples.add(validateMetadata(value, "example"));
            }
        }

        Optional<Duration> cooldownDuration = Optional.empty();
        if (cooldown != null) {
            if (cooldown.value() <= 0) {
                throw new IllegalArgumentException("cooldown must be positive");
            }
            cooldownDuration = Optional.of(Duration.ofMillis(cooldown.unit().toMillis(cooldown.value())));
        }

        return new MethodCommandBinder.CommandMetadata(
            owner.isAnnotationPresent(Hidden.class),
            usage == null ? Optional.empty() : Optional.of(validateMetadata(usage.value(), "usage")),
            examples,
            cooldownDuration,
            requirement == null ? Optional.empty() : Optional.of(validateMetadata(requirement.value(), "requirement")),
            !owner.isAnnotationPresent(SuggestAliases.class) || owner.getAnnotation(SuggestAliases.class).value(),
            List.of(),
            MethodCommandBinder.annotatedMiddlewares(owner)
        );
    }

    private static Optional<String> metadata(Description description, String label) {
        return description == null ? Optional.empty() : Optional.of(validateMetadata(description.value(), label));
    }

    private static Optional<PermissionSpec> permission(Permission permission) {
        if (permission == null) {
            return Optional.empty();
        }
        String value = validateMetadata(permission.value(), "permission");
        return Optional.of(permission.regex() ? PermissionSpec.regex(value) : PermissionSpec.exact(value));
    }

    private static String validateMetadata(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    private static CasePolicy casePolicy(CaseInsensitive caseInsensitive) {
        if (caseInsensitive == null) {
            return CasePolicy.strict();
        }
        return new CasePolicy(caseInsensitive.literals(), caseInsensitive.options());
    }

    private static int annotationCount(Command command, Route route, Subcommand subcommand, SubRoute subRoute) {
        int count = 0;
        if (command != null) {
            count++;
        }
        if (route != null) {
            count++;
        }
        if (subcommand != null) {
            count++;
        }
        if (subRoute != null) {
            count++;
        }
        return count;
    }

    private static void validateSingleLiteral(String value, String annotation, String routeAnnotation) {
        Objects.requireNonNull(value, annotation);
        String trimmed = value.trim();
        if (trimmed.isBlank() || !trimmed.equals(value) || trimmed.split("\\s+").length != 1
            || trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("[") || trimmed.contains("]")
            || trimmed.contains("|")) {
            throw new IllegalArgumentException(annotation + " only accepts one literal; use " + routeAnnotation
                + " for route DSL: " + value);
        }
    }

    private static String signature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[index].getName());
        }
        return builder.append(')').toString();
    }

    private static void validateSuggestionProviders(List<CompiledCommand> compiled, Map<String, Class<?>> routeTypes) {
        List<RouteStep> routeSteps = compiled.stream()
            .filter(command -> command.kind() == RegistrationKind.ROUTE)
            .flatMap(command -> RouteParser.parse(command.route(), routeTypes).steps().stream())
            .toList();
        compiled.stream()
            .flatMap(command -> command.metadata().suggestions().stream())
            .map(MethodCommandBinder.SuggestionBinding::name)
            .distinct()
            .filter(suggestion -> routeSteps.stream().noneMatch(step -> routeStepNameMatches(step, suggestion)))
            .findFirst()
            .ifPresent(suggestion -> {
                throw new IllegalArgumentException("@Suggest provider does not match a route argument or option: "
                    + suggestion);
            });
    }

    private static boolean routeStepNameMatches(RouteStep step, String name) {
        return step instanceof ArgumentRouteStep argument && argument.name().equals(name)
            || step instanceof OptionRouteStep option && option.name().equals(name);
    }

    public record CompiledCommands(
        CasePolicy classCasePolicy,
        Optional<RootCommand> rootCommand,
        List<CompiledCommand> commands
    ) {
        public CompiledCommands {
            Objects.requireNonNull(classCasePolicy, "classCasePolicy");
            rootCommand = Objects.requireNonNull(rootCommand, "rootCommand");
            commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        }

        public void register(CommandRegistry registry) {
            Objects.requireNonNull(registry, "registry");
            Map<String, Class<?>> routeTypes = registry.routeTypes();
            classCasePolicy.apply(registry);
            rootCommand.ifPresent(root -> root.register(registry));
            for (CompiledCommand command : commands) {
                command.register(registry, routeTypes);
            }
        }
    }

    public record RootCommand(
        String literal,
        List<String> aliases,
        Optional<String> group,
        Optional<String> description,
        Optional<PermissionSpec> permission,
        MethodCommandBinder.CommandMetadata metadata,
        CasePolicy casePolicy
    ) {
        public RootCommand {
            Objects.requireNonNull(literal, "literal");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            group = Objects.requireNonNull(group, "group");
            description = Objects.requireNonNull(description, "description");
            permission = Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(casePolicy, "casePolicy");
        }

        void register(CommandRegistry registry) {
            casePolicy.apply(registry);
            registry.command(literal, builder -> {
                if (!aliases.isEmpty()) {
                    builder.aliases(aliases.toArray(String[]::new));
                }
                description.ifPresent(builder::description);
                permission.ifPresent(value -> PermissionBinder.apply(builder, value));
                if (metadata.hidden()) {
                    builder.hidden();
                }
                metadata.usage().ifPresent(builder::usage);
                metadata.examples().forEach(builder::example);
                metadata.cooldown().ifPresent(builder::cooldown);
                metadata.requirement().ifPresent(builder::requirement);
                builder.suggestAliases(metadata.suggestAliases());
                metadata.middlewares().forEach(builder::middleware);
                group.ifPresent(builder::group);
            });
        }
    }

    public record CompiledCommand(
        RegistrationKind kind,
        String route,
        Optional<String> group,
        List<String> aliases,
        List<SubcommandSegment> subcommands,
        List<String> subcommandAliases,
        Optional<String> description,
        Optional<PermissionSpec> permission,
        CasePolicy casePolicy,
        MethodCommandBinder.BoundMethod boundMethod
    ) {
        public CompiledCommand {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(route, "route");
            group = Objects.requireNonNull(group, "group");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            subcommands = List.copyOf(Objects.requireNonNull(subcommands, "subcommands"));
            subcommandAliases = List.copyOf(Objects.requireNonNull(subcommandAliases, "subcommandAliases"));
            description = Objects.requireNonNull(description, "description");
            permission = Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(casePolicy, "casePolicy");
            Objects.requireNonNull(boundMethod, "boundMethod");
        }

        public List<MethodCommandBinder.ParameterBinding> bindings() {
            return boundMethod.bindings();
        }

        public MethodCommandBinder.CommandMetadata metadata() {
            return boundMethod.metadata();
        }

        void register(CommandRegistry registry, Map<String, Class<?>> routeTypes) {
            casePolicy.apply(registry);
            if (kind == RegistrationKind.ROUTE) {
                CommandRegistry.RouteBuilder builder = registry.route(route);
                description.ifPresent(builder::description);
                permission.ifPresent(value -> PermissionBinder.apply(builder, value));
                applyMetadata(builder, routeTypes);
                builder.executes(boundMethod::invoke);
                return;
            }

            if (kind == RegistrationKind.SUBCOMMAND) {
                registry.command(route, builder -> {
                    if (!aliases.isEmpty()) {
                        builder.aliases(aliases.toArray(String[]::new));
                    }
                    registerSubcommandPath(builder, 0);
                });
                return;
            }

            registry.command(route, builder -> {
                description.ifPresent(builder::description);
                permission.ifPresent(value -> PermissionBinder.apply(builder, value));
                applyMetadata(builder);
                if (!aliases.isEmpty()) {
                    builder.aliases(aliases.toArray(String[]::new));
                }
                builder.executes(boundMethod::invoke);
            });
        }

        private void registerSubcommandPath(CommandRegistry.CommandBuilder builder, int index) {
            SubcommandSegment segment = subcommands.get(index);
            builder.subcommand(segment.literal(), child -> {
                segment.apply(child);
                if (index == subcommands.size() - 1) {
                    description.ifPresent(child::description);
                    permission.ifPresent(value -> PermissionBinder.apply(child, value));
                    applyMetadata(child);
                    child.executes(boundMethod::invoke);
                } else {
                    registerSubcommandPath(child, index + 1);
                }
            });
        }

        private void applyMetadata(CommandRegistry.CommandBuilder builder) {
            MethodCommandBinder.CommandMetadata metadata = metadata();
            if (metadata.hidden()) {
                builder.hidden();
            }
            metadata.usage().ifPresent(builder::usage);
            metadata.examples().forEach(builder::example);
            metadata.cooldown().ifPresent(builder::cooldown);
            metadata.requirement().ifPresent(builder::requirement);
            builder.suggestAliases(metadata.suggestAliases());
            metadata.middlewares().forEach(builder::middleware);
            group.ifPresent(builder::group);
        }

        private void applyMetadata(CommandRegistry.RouteBuilder builder, Map<String, Class<?>> routeTypes) {
            MethodCommandBinder.CommandMetadata metadata = metadata();
            if (metadata.hidden()) {
                builder.hidden();
            }
            metadata.usage().ifPresent(builder::usage);
            metadata.examples().forEach(builder::example);
            metadata.cooldown().ifPresent(builder::cooldown);
            metadata.requirement().ifPresent(builder::requirement);
            builder.suggestAliases(metadata.suggestAliases());
            applyRouteSuggestions(builder, metadata.suggestions(), routeTypes);
            metadata.middlewares().forEach(builder::middleware);
            group.ifPresent(builder::group);
        }

        private void applyRouteSuggestions(CommandRegistry.RouteBuilder builder,
            List<MethodCommandBinder.SuggestionBinding> suggestions, Map<String, Class<?>> routeTypes) {
            List<RouteStep> steps = RouteParser.parse(route, routeTypes).steps();
            for (MethodCommandBinder.SuggestionBinding suggestion : suggestions) {
                for (RouteStep step : steps) {
                    if (step instanceof ArgumentRouteStep argument && argument.name().equals(suggestion.name())) {
                        builder.argumentSuggestions(suggestion.name(), suggestion.name(), suggestion.provider());
                    }
                    if (step instanceof OptionRouteStep option && option.name().equals(suggestion.name())) {
                        builder.optionSuggestions(suggestion.name(), suggestion.name(), suggestion.provider());
                    }
                }
            }
        }
    }

    public record CasePolicy(boolean literals, boolean options) {
        static CasePolicy strict() {
            return new CasePolicy(false, false);
        }

        void apply(CommandRegistry registry) {
            if (literals) {
                registry.caseInsensitiveLiterals();
            }
            if (options) {
                registry.caseInsensitiveOptions();
            }
        }
    }

    public record SubcommandSegment(
        String literal,
        List<String> aliases,
        Optional<String> description,
        Optional<PermissionSpec> permission,
        MethodCommandBinder.CommandMetadata metadata
    ) {
        public SubcommandSegment {
            Objects.requireNonNull(literal, "literal");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            description = Objects.requireNonNull(description, "description");
            permission = Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(metadata, "metadata");
        }

        String routeToken() {
            if (aliases.isEmpty()) {
                return literal;
            }
            List<String> tokens = new ArrayList<>();
            tokens.add(literal);
            tokens.addAll(aliases);
            return String.join("|", tokens);
        }

        void apply(CommandRegistry.CommandBuilder builder) {
            if (!aliases.isEmpty()) {
                builder.aliases(aliases.toArray(String[]::new));
            }
            description.ifPresent(builder::description);
            permission.ifPresent(value -> PermissionBinder.apply(builder, value));
            if (metadata.hidden()) {
                builder.hidden();
            }
            metadata.usage().ifPresent(builder::usage);
            metadata.examples().forEach(builder::example);
            metadata.cooldown().ifPresent(builder::cooldown);
            metadata.requirement().ifPresent(builder::requirement);
            builder.suggestAliases(metadata.suggestAliases());
            metadata.middlewares().forEach(builder::middleware);
        }
    }

    public enum RegistrationKind {COMMAND, ROUTE, SUBCOMMAND}
}
