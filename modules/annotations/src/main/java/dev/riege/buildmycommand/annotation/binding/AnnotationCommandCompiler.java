package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandRegistry;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AnnotationCommandCompiler {
    private AnnotationCommandCompiler() {
    }

    public static CompiledCommands compile(Object commands) {
        Objects.requireNonNull(commands, "commands");
        Class<?> owner = commands.getClass();
        CasePolicy classCasePolicy = casePolicy(owner.getAnnotation(CaseInsensitive.class));
        Optional<String> group = commandGroup(owner);
        Optional<RootCommand> rootCommand = rootCommand(owner, group, classCasePolicy);

        List<CompiledCommand> compiled = Arrays.stream(owner.getDeclaredMethods())
            .filter(method -> isAnnotatedCommandMethod(owner, method))
            .sorted(Comparator
                .comparing((Method method) -> routeKey(owner, method))
                .thenComparing(AnnotationCommandCompiler::signature))
            .map(method -> compileMethod(commands, method, group))
            .toList();

        return new CompiledCommands(classCasePolicy, rootCommand, compiled);
    }

    private static CompiledCommand compileMethod(
        Object target,
        Method method,
        Optional<String> group
    ) {
        Command command = method.getAnnotation(Command.class);
        Route route = method.getAnnotation(Route.class);
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        SubRoute subRoute = method.getAnnotation(SubRoute.class);
        Command ownerCommand = target.getClass().getAnnotation(Command.class);
        if (annotationCount(command, route, subcommand, subRoute) > 1) {
            throw new IllegalArgumentException("annotated command method cannot mix command route annotations: "
                + method.getName());
        }

        MethodCommandBinder.BoundMethod boundMethod = MethodCommandBinder.bind(target, method);
        Optional<String> description = metadata(method.getAnnotation(Description.class), "description");
        Optional<String> permission = metadata(method.getAnnotation(Permission.class), "permission");
        CasePolicy methodCasePolicy = casePolicy(method.getAnnotation(CaseInsensitive.class));

        if (ownerCommand != null) {
            validateSingleLiteral(ownerCommand.value(), "@Command", "@Route");
        }

        if (subRoute != null) {
            String commandRoute = aliasedSubcommandRoute(
                ownerCommand.value() + " " + subRoute.value(),
                target.getClass().getAnnotation(Alias.class),
                method.getAnnotation(Alias.class)
            );
            AnnotationRouteValidator.validateRouteContextUsage(commandRoute, method, boundMethod.bindings());
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                group,
                List.of(),
                Optional.empty(),
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }
        if (subcommand != null) {
            validateSingleLiteral(subcommand.value(), "@Subcommand", "@SubRoute");
            Alias ownerAlias = target.getClass().getAnnotation(Alias.class);
            Alias methodAlias = method.getAnnotation(Alias.class);
            return new CompiledCommand(
                RegistrationKind.SUBCOMMAND,
                ownerCommand.value(),
                group,
                ownerAlias == null ? List.of() : List.of(ownerAlias.value()),
                Optional.of(subcommand.value()),
                methodAlias == null ? List.of() : List.of(methodAlias.value()),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }
        if (route != null) {
            String commandRoute = aliasedRoute(route.value(), method.getAnnotation(Alias.class));
            AnnotationRouteValidator.validateRouteContextUsage(commandRoute, method, boundMethod.bindings());
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                group,
                List.of(),
                Optional.empty(),
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
            group,
            alias == null ? List.of() : List.of(alias.value()),
            Optional.empty(),
            List.of(),
            description,
            permission,
            methodCasePolicy,
            boundMethod
        );
    }

    private static boolean isAnnotatedCommandMethod(Class<?> owner, Method method) {
        return method.isAnnotationPresent(Command.class)
            || method.isAnnotationPresent(Route.class)
            || (owner.isAnnotationPresent(Command.class)
                && (method.isAnnotationPresent(Subcommand.class) || method.isAnnotationPresent(SubRoute.class)));
    }

    private static String routeKey(Class<?> owner, Method method) {
        Command command = method.getAnnotation(Command.class);
        if (command != null) {
            return command.value();
        }
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        if (subcommand != null) {
            return owner.getAnnotation(Command.class).value() + " " + subcommand.value();
        }
        SubRoute subRoute = method.getAnnotation(SubRoute.class);
        if (subRoute != null) {
            return owner.getAnnotation(Command.class).value() + " " + subRoute.value();
        }
        return method.getAnnotation(Route.class).value();
    }

    private static Optional<String> commandGroup(Class<?> owner) {
        CommandGroup group = owner.getAnnotation(CommandGroup.class);
        return group == null ? Optional.empty() : Optional.of(validateMetadata(group.value(), "command group"));
    }

    private static Optional<RootCommand> rootCommand(Class<?> owner, Optional<String> group, CasePolicy casePolicy) {
        Command command = owner.getAnnotation(Command.class);
        if (command == null || !hasRootMetadata(owner)) {
            return Optional.empty();
        }
        validateSingleLiteral(command.value(), "@Command", "@Route");
        Alias alias = owner.getAnnotation(Alias.class);
        return Optional.of(new RootCommand(
            command.value(),
            alias == null ? List.of() : List.of(alias.value()),
            group,
            metadata(owner.getAnnotation(Description.class), "description"),
            metadata(owner.getAnnotation(Permission.class), "permission"),
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
            || owner.isAnnotationPresent(CommandGroup.class);
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
            requirement == null ? Optional.empty() : Optional.of(validateMetadata(requirement.value(), "requirement"))
        );
    }

    private static Optional<String> metadata(Description description, String label) {
        return description == null ? Optional.empty() : Optional.of(validateMetadata(description.value(), label));
    }

    private static Optional<String> metadata(Permission permission, String label) {
        return permission == null ? Optional.empty() : Optional.of(validateMetadata(permission.value(), label));
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

    private static String aliasedRoute(String route, Alias... aliases) {
        String aliased = route;
        for (Alias alias : aliases) {
            if (alias == null) {
                continue;
            }
            for (String value : alias.value()) {
                aliased = applyRouteAlias(aliased, value, 0);
            }
        }
        return aliased;
    }

    private static String aliasedSubcommandRoute(String route, Alias ownerAlias, Alias methodAlias) {
        String aliased = route;
        if (ownerAlias != null) {
            for (String value : ownerAlias.value()) {
                aliased = applyRouteAlias(aliased, value, 0);
            }
        }
        if (methodAlias != null) {
            for (String value : methodAlias.value()) {
                aliased = applyRouteAlias(aliased, value, 1);
            }
        }
        return aliased;
    }

    private static String applyRouteAlias(String route, String alias, int offset) {
        String[] routeTokens = route.trim().split("\\s+");
        String[] aliasTokens = alias.trim().split("\\s+");
        if (alias.isBlank()) {
            throw new IllegalArgumentException("route alias must not be blank");
        }
        if (offset + aliasTokens.length > routeTokens.length) {
            throw new IllegalArgumentException("route alias is longer than route: " + alias);
        }

        String[] updated = routeTokens.clone();
        for (int index = 0; index < aliasTokens.length; index++) {
            int routeIndex = offset + index;
            if (routeTokens[routeIndex].startsWith("<") || routeTokens[routeIndex].startsWith("[")) {
                throw new IllegalArgumentException("route alias can only target literal tokens: " + alias);
            }
            updated[routeIndex] = routeTokens[routeIndex] + "|" + aliasTokens[index];
        }
        return String.join(" ", updated);
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
            classCasePolicy.apply(registry);
            rootCommand.ifPresent(root -> root.register(registry));
            for (CompiledCommand command : commands) {
                command.register(registry);
            }
        }
    }

    public record RootCommand(
        String literal,
        List<String> aliases,
        Optional<String> group,
        Optional<String> description,
        Optional<String> permission,
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
                permission.ifPresent(builder::permission);
                if (metadata.hidden()) {
                    builder.hidden();
                }
                metadata.usage().ifPresent(builder::usage);
                metadata.examples().forEach(builder::example);
                metadata.cooldown().ifPresent(builder::cooldown);
                metadata.requirement().ifPresent(builder::requirement);
                group.ifPresent(builder::group);
            });
        }
    }

    public record CompiledCommand(
        RegistrationKind kind,
        String route,
        Optional<String> group,
        List<String> aliases,
        Optional<String> subcommand,
        List<String> subcommandAliases,
        Optional<String> description,
        Optional<String> permission,
        CasePolicy casePolicy,
        MethodCommandBinder.BoundMethod boundMethod
    ) {
        public CompiledCommand {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(route, "route");
            group = Objects.requireNonNull(group, "group");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            subcommand = Objects.requireNonNull(subcommand, "subcommand");
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

        void register(CommandRegistry registry) {
            casePolicy.apply(registry);
            if (kind == RegistrationKind.ROUTE) {
                CommandRegistry.RouteBuilder builder = registry.route(route);
                description.ifPresent(builder::description);
                permission.ifPresent(builder::permission);
                applyMetadata(builder);
                builder.executes(boundMethod::invoke);
                return;
            }

            if (kind == RegistrationKind.SUBCOMMAND) {
                registry.command(route, builder -> {
                    if (!aliases.isEmpty()) {
                        builder.aliases(aliases.toArray(String[]::new));
                    }
                    builder.subcommand(subcommand.orElseThrow(), child -> {
                        description.ifPresent(child::description);
                        permission.ifPresent(child::permission);
                        applyMetadata(child);
                        if (!subcommandAliases.isEmpty()) {
                            child.aliases(subcommandAliases.toArray(String[]::new));
                        }
                        applyBindings(child);
                        child.executes(boundMethod::invoke);
                    });
                });
                return;
            }

            registry.command(route, builder -> {
                description.ifPresent(builder::description);
                permission.ifPresent(builder::permission);
                applyMetadata(builder);
                if (!aliases.isEmpty()) {
                    builder.aliases(aliases.toArray(String[]::new));
                }
                applyBindings(builder);
                builder.executes(boundMethod::invoke);
            });
        }

        private void applyBindings(CommandRegistry.CommandBuilder builder) {
            for (MethodCommandBinder.ParameterBinding binding : boundMethod.bindings()) {
                binding.apply(builder);
                binding.suggestionProviderFunction().ifPresent(provider -> {
                    if (binding.kind() == MethodCommandBinder.Kind.ARGUMENT) {
                        builder.argumentSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                    } else {
                        builder.optionSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                    }
                });
            }
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
            group.ifPresent(builder::group);
        }

        private void applyMetadata(CommandRegistry.RouteBuilder builder) {
            MethodCommandBinder.CommandMetadata metadata = metadata();
            if (metadata.hidden()) {
                builder.hidden();
            }
            metadata.usage().ifPresent(builder::usage);
            metadata.examples().forEach(builder::example);
            metadata.cooldown().ifPresent(builder::cooldown);
            metadata.requirement().ifPresent(builder::requirement);
            group.ifPresent(builder::group);
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

    public enum RegistrationKind {
        COMMAND,
        ROUTE,
        SUBCOMMAND
    }
}
