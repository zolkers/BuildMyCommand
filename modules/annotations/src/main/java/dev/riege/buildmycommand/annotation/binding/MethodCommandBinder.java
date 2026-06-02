package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Default;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Flag;
import dev.riege.buildmycommand.annotation.Greedy;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Option;
import dev.riege.buildmycommand.annotation.OptionalArg;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MethodCommandBinder {
    private MethodCommandBinder() {
    }

    public static BoundMethod bind(Object target, Method method) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(method, "method");
        validateMethod(method);
        List<ParameterBinding> bindings = bindingsFor(method);
        makeAccessible(target, method);
        return new BoundMethod(target, method, bindings, metadataFor(target.getClass(), method));
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

    private static void makeAccessible(Object target, Method method) {
        try {
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
        } catch (SecurityException exception) {
            throw new IllegalStateException("cannot access annotated command method: " + method.getName(), exception);
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
        if (type == CommandContext.class && arg == null && flag == null && option == null) {
            return ParameterBinding.context();
        }
        if (arg != null && isSupportedArgumentType(type)) {
            return ParameterBinding.argument(
                arg.value(),
                type,
                parameter.isAnnotationPresent(OptionalArg.class),
                parameter.isAnnotationPresent(Greedy.class),
                defaultValue(parameter),
                suggestionProvider(parameter),
                "Arg"
            );
        }
        if (flag != null && (type == boolean.class || type == Boolean.class)) {
            return ParameterBinding.flag(flag.value(), aliasValue(parameter), "Flag");
        }
        if (option != null && isSupportedArgumentType(type)) {
            return ParameterBinding.option(option.value(), type, aliasValue(parameter), suggestionProvider(parameter), "Option");
        }
        if (option != null && type == Optional.class) {
            Class<?> valueType = optionalValueType(parameter);
            if (valueType != null && isSupportedArgumentType(valueType)) {
                return ParameterBinding.optionalOption(
                    option.value(),
                    valueType,
                    aliasValue(parameter),
                    suggestionProvider(parameter),
                    "Option"
                );
            }
        }
        if (arg == null && flag == null && option == null && isSupportedArgumentType(type)) {
            if (!parameter.isNamePresent()) {
                throw new IllegalArgumentException("cannot infer annotated command parameter name: "
                    + bindingName(parameter));
            }
            return ParameterBinding.argument(
                parameter.getName(),
                type,
                parameter.isAnnotationPresent(OptionalArg.class),
                parameter.isAnnotationPresent(Greedy.class),
                defaultValue(parameter),
                suggestionProvider(parameter),
                null
            );
        }

        throw new IllegalArgumentException("unsupported annotated command parameter: " + bindingName(parameter));
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

    private static String defaultValue(Parameter parameter) {
        Default defaultAnnotation = parameter.getAnnotation(Default.class);
        return defaultAnnotation == null ? null : defaultAnnotation.value();
    }

    private static String aliasValue(Parameter parameter) {
        Alias alias = parameter.getAnnotation(Alias.class);
        if (alias == null || alias.value().length == 0) {
            return null;
        }
        if (alias.value().length > 1) {
            throw new IllegalArgumentException("parameter alias must contain exactly one value: " + bindingName(parameter));
        }
        return alias.value()[0];
    }

    private static Optional<String> suggestionProvider(Parameter parameter) {
        Suggest suggest = parameter.getAnnotation(Suggest.class);
        if (suggest == null) {
            return Optional.empty();
        }
        return Optional.of(metadata(suggest.value(), "suggestion provider"));
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

    private static CommandMetadata metadataFor(Class<?> owner, Method method) {
        Usage usage = method.getAnnotation(Usage.class);
        Example example = method.getAnnotation(Example.class);
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        Require requirement = method.getAnnotation(Require.class);
        if (requirement == null) {
            requirement = owner.getAnnotation(Require.class);
        }

        List<String> examples = new ArrayList<>();
        if (example != null) {
            for (String value : example.value()) {
                examples.add(metadata(value, "example"));
            }
        }

        Optional<Duration> cooldownDuration = Optional.empty();
        if (cooldown != null) {
            if (cooldown.value() <= 0) {
                throw new IllegalArgumentException("cooldown must be positive");
            }
            cooldownDuration = Optional.of(Duration.ofMillis(cooldown.unit().toMillis(cooldown.value())));
        }

        return new CommandMetadata(
            owner.isAnnotationPresent(Hidden.class) || method.isAnnotationPresent(Hidden.class),
            usage == null ? Optional.empty() : Optional.of(metadata(usage.value(), "usage")),
            examples,
            cooldownDuration,
            requirement == null ? Optional.empty() : Optional.of(metadata(requirement.value(), "requirement"))
        );
    }

    private static String metadata(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    public record BoundMethod(
        Object target,
        Method method,
        List<ParameterBinding> bindings,
        CommandMetadata metadata
    ) {
        public BoundMethod {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(method, "method");
            bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
            Objects.requireNonNull(metadata, "metadata");
        }

        public CommandResult invoke(CommandContext context) {
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
    }

    public record CommandMetadata(
        boolean hidden,
        Optional<String> usage,
        List<String> examples,
        Optional<Duration> cooldown,
        Optional<String> requirement
    ) {
        public CommandMetadata {
            usage = Objects.requireNonNull(usage, "usage");
            examples = List.copyOf(Objects.requireNonNull(examples, "examples"));
            cooldown = Objects.requireNonNull(cooldown, "cooldown");
            requirement = Objects.requireNonNull(requirement, "requirement");
        }
    }

    public record ParameterBinding(
        String name,
        Class<?> type,
        Kind kind,
        boolean optional,
        boolean greedy,
        String defaultValue,
        String alias,
        Optional<String> suggestionProvider,
        String annotationName
    ) {
        public ParameterBinding {
            suggestionProvider = Objects.requireNonNull(suggestionProvider, "suggestionProvider");
        }

        static ParameterBinding context() {
            return new ParameterBinding(null, CommandContext.class, Kind.CONTEXT, false, false, null, null,
                Optional.empty(), null);
        }

        static ParameterBinding argument(
            String name,
            Class<?> type,
            boolean optional,
            boolean greedy,
            String defaultValue,
            Optional<String> suggestionProvider,
            String annotationName
        ) {
            return new ParameterBinding(name, type, Kind.ARGUMENT, optional, greedy, defaultValue, null,
                suggestionProvider, annotationName);
        }

        static ParameterBinding flag(String name, String alias, String annotationName) {
            return new ParameterBinding(name, Boolean.class, Kind.FLAG, false, false, null, alias, Optional.empty(),
                annotationName);
        }

        static ParameterBinding option(
            String name,
            Class<?> type,
            String alias,
            Optional<String> suggestionProvider,
            String annotationName
        ) {
            return new ParameterBinding(name, type, Kind.OPTION, false, false, null, alias, suggestionProvider,
                annotationName);
        }

        static ParameterBinding optionalOption(
            String name,
            Class<?> type,
            String alias,
            Optional<String> suggestionProvider,
            String annotationName
        ) {
            return new ParameterBinding(name, type, Kind.OPTIONAL_OPTION, false, false, null, alias, suggestionProvider,
                annotationName);
        }

        public void apply(CommandRegistry.CommandBuilder builder) {
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
                builder.flag(name, alias);
            } else if (kind == Kind.OPTION || kind == Kind.OPTIONAL_OPTION) {
                builder.option(name, type, alias);
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

    public enum Kind {
        CONTEXT,
        ARGUMENT,
        FLAG,
        OPTION,
        OPTIONAL_OPTION
    }
}
