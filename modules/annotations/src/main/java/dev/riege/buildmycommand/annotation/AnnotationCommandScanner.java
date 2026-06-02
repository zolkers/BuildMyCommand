package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AnnotationCommandScanner {
    private AnnotationCommandScanner() {
    }

    public static void register(CommandRegistry registry, Object commands) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(commands, "commands");

        Arrays.stream(commands.getClass().getDeclaredMethods())
            .filter(method -> isAnnotatedCommandMethod(commands.getClass(), method))
            .sorted(Comparator
                .comparing((Method method) -> routeKey(commands.getClass(), method))
                .thenComparing(AnnotationCommandScanner::signature))
            .forEach(method -> registerMethod(registry, commands, method));
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

    private static void registerMethod(CommandRegistry registry, Object target, Method method) {
        Command command = method.getAnnotation(Command.class);
        Route route = method.getAnnotation(Route.class);
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        Command ownerCommand = target.getClass().getAnnotation(Command.class);
        if (annotationCount(command, route, subcommand) > 1) {
            throw new IllegalArgumentException("annotated command method cannot use both @Command and @Route: "
                + method.getName());
        }
        validateMethod(method);
        List<ParameterBinding> bindings = bindingsFor(method);
        makeAccessible(target, method);

        if (subcommand != null && ownerCommand != null) {
            registerRouteMethod(registry, target, method, ownerCommand.value() + " " + subcommand.value(), bindings);
        } else if (route != null) {
            registerRouteMethod(registry, target, method, route, bindings);
        } else {
            registerCommandMethod(registry, target, method, command, bindings);
        }
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

    private static void registerCommandMethod(
        CommandRegistry registry,
        Object target,
        Method method,
        Command command,
        List<ParameterBinding> bindings
    ) {
        registry.command(command.value(), builder -> {
            applyMetadata(method, builder);
            Alias alias = method.getAnnotation(Alias.class);
            if (alias != null) {
                builder.aliases(alias.value());
            }
            for (ParameterBinding binding : bindings) {
                binding.apply(builder);
            }
            builder.executes(context -> invoke(target, method, bindings, context));
        });
    }

    private static void registerRouteMethod(
        CommandRegistry registry,
        Object target,
        Method method,
        Route route,
        List<ParameterBinding> bindings
    ) {
        registerRouteMethod(registry, target, method, route.value(), bindings);
    }

    private static void registerRouteMethod(
        CommandRegistry registry,
        Object target,
        Method method,
        String route,
        List<ParameterBinding> bindings
    ) {
        CommandRegistry.RouteBuilder builder = registry.route(route);
        Description description = method.getAnnotation(Description.class);
        if (description != null) {
            builder.description(description.value());
        }
        Permission permission = method.getAnnotation(Permission.class);
        if (permission != null) {
            builder.permission(permission.value());
        }
        builder.executes(context -> invoke(target, method, bindings, context));
    }

    private static void applyMetadata(Method method, CommandRegistry.CommandBuilder builder) {
        Description description = method.getAnnotation(Description.class);
        if (description != null) {
            builder.description(description.value());
        }
        Permission permission = method.getAnnotation(Permission.class);
        if (permission != null) {
            builder.permission(permission.value());
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

    private static void makeAccessible(Object target, Method method) {
        try {
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
        } catch (SecurityException exception) {
            throw new IllegalStateException("cannot access annotated command method: " + method.getName(), exception);
        }
    }

    private static void validateMethod(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)) {
            throw new IllegalArgumentException("annotated command method must be public or package-private: "
                + method.getName());
        }
        if (method.getReturnType() != CommandResult.class) {
            throw new IllegalArgumentException("annotated command method must return CommandResult: "
                + method.getName());
        }
    }

    private static List<ParameterBinding> bindingsFor(Method method) {
        List<ParameterBinding> bindings = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            bindings.add(bindingFor(parameter));
        }
        return List.copyOf(bindings);
    }

    private static ParameterBinding bindingFor(Parameter parameter) {
        Arg arg = parameter.getAnnotation(Arg.class);
        Flag flag = parameter.getAnnotation(Flag.class);
        Option option = parameter.getAnnotation(Option.class);

        if (annotationCount(arg, flag, option) > 1) {
            throw new IllegalArgumentException("unsupported annotated command parameter: " + bindingName(parameter));
        }

        Class<?> type = parameter.getType();
        if (type == CommandContext.class && arg == null && flag == null) {
            return ParameterBinding.context();
        }
        if (arg != null && isSupportedArgumentType(type)) {
            return ParameterBinding.argument(
                arg.value(),
                type,
                parameter.isAnnotationPresent(OptionalArg.class),
                parameter.isAnnotationPresent(Greedy.class),
                defaultValue(parameter)
            );
        }
        if (flag != null && (type == boolean.class || type == Boolean.class)) {
            return ParameterBinding.flag(flag.value());
        }
        if (option != null && isSupportedArgumentType(type)) {
            return ParameterBinding.option(option.value(), type);
        }
        if (option != null && type == Optional.class) {
            Class<?> valueType = optionalValueType(parameter);
            if (valueType != null && isSupportedArgumentType(valueType)) {
                return ParameterBinding.optionalOption(option.value(), valueType);
            }
        }

        throw new IllegalArgumentException("unsupported annotated command parameter: " + bindingName(parameter));
    }

    private static String defaultValue(Parameter parameter) {
        Default defaultAnnotation = parameter.getAnnotation(Default.class);
        return defaultAnnotation == null ? null : defaultAnnotation.value();
    }

    private static int annotationCount(Arg arg, Flag flag, Option option) {
        int count = 0;
        if (arg != null) {
            count++;
        }
        if (flag != null) {
            count++;
        }
        if (option != null) {
            count++;
        }
        return count;
    }

    private static String bindingName(Parameter parameter) {
        Arg arg = parameter.getAnnotation(Arg.class);
        if (arg != null) {
            return arg.value();
        }
        Flag flag = parameter.getAnnotation(Flag.class);
        if (flag != null) {
            return flag.value();
        }
        Option option = parameter.getAnnotation(Option.class);
        if (option != null) {
            return option.value();
        }
        return parameter.getName();
    }

    private static boolean isSupportedArgumentType(Class<?> type) {
        return type == String.class
            || type == Integer.class
            || type == int.class
            || type == Long.class
            || type == long.class
            || type == Double.class
            || type == double.class
            || type == Boolean.class
            || type == boolean.class
            || type == UUID.class
            || type.isEnum();
    }

    private static Class<?> optionalValueType(Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type valueType = parameterizedType.getActualTypeArguments()[0];
        return valueType instanceof Class<?> valueClass ? valueClass : null;
    }

    private static CommandResult invoke(
        Object target,
        Method method,
        List<ParameterBinding> bindings,
        CommandContext context
    ) {
        Object[] arguments = new Object[bindings.size()];
        for (int index = 0; index < bindings.size(); index++) {
            arguments[index] = bindings.get(index).value(context);
        }

        try {
            return (CommandResult) method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot invoke annotated command method: " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("annotated command method failed: " + method.getName(), cause);
        }
    }

    private record ParameterBinding(String name, Class<?> type, Kind kind, boolean optional, boolean greedy, String defaultValue) {
        static ParameterBinding context() {
            return new ParameterBinding(null, CommandContext.class, Kind.CONTEXT, false, false, null);
        }

        static ParameterBinding argument(String name, Class<?> type, boolean optional, boolean greedy, String defaultValue) {
            return new ParameterBinding(name, type, Kind.ARGUMENT, optional, greedy, defaultValue);
        }

        static ParameterBinding flag(String name) {
            return new ParameterBinding(name, Boolean.class, Kind.FLAG, false, false, null);
        }

        static ParameterBinding option(String name, Class<?> type) {
            return new ParameterBinding(name, type, Kind.OPTION, false, false, null);
        }

        static ParameterBinding optionalOption(String name, Class<?> type) {
            return new ParameterBinding(name, type, Kind.OPTIONAL_OPTION, false, false, null);
        }

        void apply(CommandRegistry.CommandBuilder builder) {
            if (kind == Kind.ARGUMENT) {
                if (greedy && optional) {
                    builder.optionalGreedyArgument(name, type);
                } else if (greedy) {
                    builder.greedyArgument(name, type);
                } else if (optional) {
                    builder.optionalArgument(name, type);
                } else {
                    builder.argument(name, type);
                }
            } else if (kind == Kind.FLAG) {
                builder.flag(name);
            } else if (kind == Kind.OPTION || kind == Kind.OPTIONAL_OPTION) {
                builder.option(name, type);
            }
        }

        Object value(CommandContext context) {
            return switch (kind) {
                case CONTEXT -> context;
                case ARGUMENT -> argumentValue(context);
                case FLAG -> context.flag(name);
                case OPTION -> context.option(name, type).orElse(null);
                case OPTIONAL_OPTION -> context.option(name, type);
            };
        }

        private Object parsedDefault() {
            if (defaultValue == null) {
                return null;
            }
            if (type == String.class) {
                return defaultValue;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(defaultValue);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(defaultValue);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(defaultValue);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.valueOf(defaultValue);
            }
            return defaultValue;
        }

        private Object argumentValue(CommandContext context) {
            if (!optional && defaultValue == null) {
                return context.arg(name, type);
            }
            java.util.Optional<?> value = context.optionalArg(name, type);
            return value.isPresent() ? value.get() : parsedDefault();
        }
    }

    private enum Kind {
        CONTEXT,
        ARGUMENT,
        FLAG,
        OPTION,
        OPTIONAL_OPTION
    }
}
