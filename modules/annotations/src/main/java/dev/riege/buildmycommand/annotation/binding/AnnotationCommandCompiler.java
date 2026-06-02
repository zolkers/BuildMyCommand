package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.api.CommandRegistry;

import java.lang.reflect.Method;
import java.util.Arrays;
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

        List<CompiledCommand> compiled = Arrays.stream(owner.getDeclaredMethods())
            .filter(method -> isAnnotatedCommandMethod(owner, method))
            .sorted(Comparator
                .comparing((Method method) -> routeKey(owner, method))
                .thenComparing(AnnotationCommandCompiler::signature))
            .map(method -> compileMethod(commands, method, group))
            .toList();

        return new CompiledCommands(classCasePolicy, compiled);
    }

    private static CompiledCommand compileMethod(
        Object target,
        Method method,
        Optional<String> group
    ) {
        Command command = method.getAnnotation(Command.class);
        Route route = method.getAnnotation(Route.class);
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        Command ownerCommand = target.getClass().getAnnotation(Command.class);
        if (annotationCount(command, route, subcommand) > 1) {
            throw new IllegalArgumentException("annotated command method cannot use both @Command and @Route: "
                + method.getName());
        }

        MethodCommandBinder.BoundMethod boundMethod = MethodCommandBinder.bind(target, method);
        Optional<String> description = metadata(method.getAnnotation(Description.class), "description");
        Optional<String> permission = metadata(method.getAnnotation(Permission.class), "permission");
        CasePolicy methodCasePolicy = casePolicy(method.getAnnotation(CaseInsensitive.class));

        if (subcommand != null && ownerCommand != null) {
            String commandRoute = aliasedSubcommandRoute(
                ownerCommand.value() + " " + subcommand.value(),
                target.getClass().getAnnotation(Alias.class),
                method.getAnnotation(Alias.class)
            );
            AnnotationRouteValidator.validate(commandRoute, method, boundMethod.bindings());
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                group,
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }
        if (route != null) {
            String commandRoute = aliasedRoute(route.value(), method.getAnnotation(Alias.class));
            AnnotationRouteValidator.validate(commandRoute, method, boundMethod.bindings());
            return new CompiledCommand(
                RegistrationKind.ROUTE,
                commandRoute,
                group,
                List.of(),
                description,
                permission,
                methodCasePolicy,
                boundMethod
            );
        }

        Alias alias = method.getAnnotation(Alias.class);
        return new CompiledCommand(
            RegistrationKind.COMMAND,
            command.value(),
            group,
            alias == null ? List.of() : List.of(alias.value()),
            description,
            permission,
            methodCasePolicy,
            boundMethod
        );
    }

    private static boolean isAnnotatedCommandMethod(Class<?> owner, Method method) {
        return method.isAnnotationPresent(Command.class)
            || method.isAnnotationPresent(Route.class)
            || (owner.isAnnotationPresent(Command.class) && method.isAnnotationPresent(Subcommand.class));
    }

    private static String routeKey(Class<?> owner, Method method) {
        Command command = method.getAnnotation(Command.class);
        if (command != null) {
            return command.value();
        }
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        if (subcommand != null && owner.isAnnotationPresent(Command.class)) {
            return owner.getAnnotation(Command.class).value() + " " + subcommand.value();
        }
        return method.getAnnotation(Route.class).value();
    }

    private static Optional<String> commandGroup(Class<?> owner) {
        CommandGroup group = owner.getAnnotation(CommandGroup.class);
        return group == null ? Optional.empty() : Optional.of(validateMetadata(group.value(), "command group"));
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

    private static int annotationCount(Command command, Route route, Subcommand subcommand) {
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
        return count;
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
        if (routeTokens.length == 0 || aliasTokens.length == 0 || alias.isBlank()) {
            throw new IllegalArgumentException("route alias must not be blank");
        }
        if (offset + aliasTokens.length > routeTokens.length) {
            throw new IllegalArgumentException("route alias is longer than route: " + alias);
        }

        String[] updated = routeTokens.clone();
        for (int index = 0; index < aliasTokens.length; index++) {
            int routeIndex = offset + index;
            if (routeTokens[routeIndex].startsWith("<") || routeTokens[routeIndex].startsWith("[")
                || routeTokens[routeIndex].endsWith(">") || routeTokens[routeIndex].endsWith("]")) {
                throw new IllegalArgumentException("route alias can only target literal tokens: " + alias);
            }
            updated[routeIndex] = routeTokens[routeIndex] + "|" + aliasTokens[index];
        }
        return String.join(" ", updated);
    }

    private static String aliasedParameterRoute(
        String route,
        List<MethodCommandBinder.ParameterBinding> bindings
    ) {
        String aliased = route;
        for (MethodCommandBinder.ParameterBinding binding : bindings) {
            if (binding.kind() == MethodCommandBinder.Kind.FLAG
                || binding.kind() == MethodCommandBinder.Kind.OPTION
                || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) {
                aliased = applyOptionAlias(aliased, binding.name(), binding.alias());
            }
        }
        return aliased;
    }

    private static String applyOptionAlias(String route, String optionName, String alias) {
        if (alias == null) {
            return route;
        }

        String[] tokens = route.split("\\s+");
        for (int index = 0; index < tokens.length; index++) {
            String token = tokens[index];
            if (!token.startsWith("[--") || !token.endsWith("]") || token.contains("|")) {
                continue;
            }
            String body = token.substring(1, token.length() - 1);
            int separator = body.indexOf(':');
            String longName = separator < 0 ? body.substring(2) : body.substring(2, separator);
            if (longName.equals(optionName)) {
                tokens[index] = token.substring(0, token.length() - 1) + "|-" + alias + "]";
            }
        }
        return String.join(" ", tokens);
    }

    public record CompiledCommands(CasePolicy classCasePolicy, List<CompiledCommand> commands) {
        public CompiledCommands {
            Objects.requireNonNull(classCasePolicy, "classCasePolicy");
            commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        }

        public void register(CommandRegistry registry) {
            Objects.requireNonNull(registry, "registry");
            classCasePolicy.apply(registry);
            for (CompiledCommand command : commands) {
                command.register(registry);
            }
        }
    }

    public record CompiledCommand(
        RegistrationKind kind,
        String route,
        Optional<String> group,
        List<String> aliases,
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
                CommandRegistry.RouteBuilder builder = registry.route(aliasedParameterRoute(route, boundMethod.bindings()));
                description.ifPresent(builder::description);
                permission.ifPresent(builder::permission);
                applyMetadata(builder);
                for (MethodCommandBinder.ParameterBinding binding : boundMethod.bindings()) {
                    binding.suggestionProviderFunction().ifPresent(provider -> {
                        if (binding.kind() == MethodCommandBinder.Kind.ARGUMENT) {
                            builder.argumentSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                        } else if (binding.kind() == MethodCommandBinder.Kind.OPTION
                            || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) {
                            builder.optionSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                        }
                    });
                }
                builder.executes(boundMethod::invoke);
                return;
            }

            registry.command(route, builder -> {
                description.ifPresent(builder::description);
                permission.ifPresent(builder::permission);
                applyMetadata(builder);
                if (!aliases.isEmpty()) {
                    builder.aliases(aliases.toArray(String[]::new));
                }
                for (MethodCommandBinder.ParameterBinding binding : boundMethod.bindings()) {
                    binding.apply(builder);
                    binding.suggestionProviderFunction().ifPresent(provider -> {
                        if (binding.kind() == MethodCommandBinder.Kind.ARGUMENT) {
                            builder.argumentSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                        } else if (binding.kind() == MethodCommandBinder.Kind.OPTION
                            || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) {
                            builder.optionSuggestions(binding.name(), binding.suggestionProvider().orElse(null), provider);
                        }
                    });
                }
                builder.executes(boundMethod::invoke);
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
        ROUTE
    }
}
