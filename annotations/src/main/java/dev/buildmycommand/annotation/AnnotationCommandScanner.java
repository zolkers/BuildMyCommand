package dev.buildmycommand.annotation;

import dev.buildmycommand.api.CommandContext;
import dev.buildmycommand.api.CommandRegistry;
import dev.buildmycommand.api.CommandResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AnnotationCommandScanner {
    private AnnotationCommandScanner() {
    }

    public static void register(CommandRegistry registry, Object commands) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(commands, "commands");

        Arrays.stream(commands.getClass().getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Command.class))
            .sorted(Comparator
                .comparing((Method method) -> method.getAnnotation(Command.class).value())
                .thenComparing(AnnotationCommandScanner::signature))
            .forEach(method -> registerMethod(registry, commands, method, method.getAnnotation(Command.class)));
    }

    private static void registerMethod(CommandRegistry registry, Object target, Method method, Command command) {
        validateMethod(method);
        List<ParameterBinding> bindings = bindingsFor(method);
        makeAccessible(target, method);

        registry.command(command.value(), builder -> {
            for (ParameterBinding binding : bindings) {
                binding.apply(builder);
            }
            builder.executes(context -> invoke(target, method, bindings, context));
        });
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

        if (arg != null && flag != null) {
            throw new IllegalArgumentException("unsupported annotated command parameter: " + arg.value());
        }

        Class<?> type = parameter.getType();
        if (type == CommandContext.class && arg == null && flag == null) {
            return ParameterBinding.context();
        }
        if (arg != null && isSupportedArgumentType(type)) {
            return ParameterBinding.argument(arg.value(), type);
        }
        if (flag != null && (type == boolean.class || type == Boolean.class)) {
            return ParameterBinding.flag(flag.value());
        }

        String name = arg != null ? arg.value() : flag != null ? flag.value() : parameter.getName();
        throw new IllegalArgumentException("unsupported annotated command parameter: " + name);
    }

    private static boolean isSupportedArgumentType(Class<?> type) {
        return type == String.class || type == Integer.class || type == int.class;
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

    private record ParameterBinding(String name, Class<?> type, Kind kind) {
        static ParameterBinding context() {
            return new ParameterBinding(null, CommandContext.class, Kind.CONTEXT);
        }

        static ParameterBinding argument(String name, Class<?> type) {
            return new ParameterBinding(name, type, Kind.ARGUMENT);
        }

        static ParameterBinding flag(String name) {
            return new ParameterBinding(name, Boolean.class, Kind.FLAG);
        }

        void apply(CommandRegistry.CommandBuilder builder) {
            if (kind == Kind.ARGUMENT) {
                builder.argument(name, type);
            } else if (kind == Kind.FLAG) {
                builder.flag(name);
            }
        }

        Object value(CommandContext context) {
            return switch (kind) {
                case CONTEXT -> context;
                case ARGUMENT -> context.arg(name, type);
                case FLAG -> context.flag(name);
            };
        }
    }

    private enum Kind {
        CONTEXT,
        ARGUMENT,
        FLAG
    }
}
