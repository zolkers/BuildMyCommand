package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.SuggestAliases;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionProvider;
import dev.riege.buildmycommand.api.SuggestionSet;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MethodCommandBinder {
    private MethodCommandBinder() {
    }

    public static BoundMethod bind(Object target, Method method) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(method, "method");
        validateMethod(method);
        List<ParameterBinding> bindings = bindingsFor(method);
        makeAccessible(target, method);
        return new BoundMethod(target, method, bindings, metadataFor(target, method));
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
        if (!method.canAccess(target)) {
            method.setAccessible(true);
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
        Class<?> type = parameter.getType();
        boolean routeContext = parameter.isAnnotationPresent(RouteCtx.class);
        if (routeContext && type == CommandContext.class) {
            return ParameterBinding.routeContext();
        }
        if (routeContext) {
            throw new IllegalArgumentException("@RouteCtx parameter must be CommandContext: routeContext");
        }
        if (type == CommandContext.class) {
            return ParameterBinding.context();
        }
        throw new IllegalArgumentException("annotated command methods only support CommandContext or @RouteCtx CommandContext parameters: "
            + parameter.getName());
    }

    private static CommandMetadata metadataFor(Object target, Method method) {
        Class<?> owner = target.getClass();
        Usage usage = method.getAnnotation(Usage.class);
        Example example = method.getAnnotation(Example.class);
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        Require requirement = method.getAnnotation(Require.class);
        if (requirement == null) {
            requirement = owner.getAnnotation(Require.class);
        }
        SuggestAliases suggestAliases = method.getAnnotation(SuggestAliases.class);
        if (suggestAliases == null) {
            suggestAliases = owner.getAnnotation(SuggestAliases.class);
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
            requirement == null ? Optional.empty() : Optional.of(metadata(requirement.value(), "requirement")),
            suggestAliases == null || suggestAliases.value(),
            suggestionsFor(target),
            annotatedMiddlewares(owner, method)
        );
    }

    private static List<SuggestionBinding> suggestionsFor(Object target) {
        return suggestionsFor(target.getClass(), target);
    }

    private static List<SuggestionBinding> suggestionsFor(Class<?> owner, Object target) {
        List<SuggestionBinding> suggestions = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods()) {
            Suggest suggest = method.getAnnotation(Suggest.class);
            if (suggest == null) {
                continue;
            }
            String name = metadata(suggest.value(), "suggestion provider");
            validateSuggestionProvider(method);
            if (target != null) {
                makeAccessible(target, method);
            }
            suggestions.add(new SuggestionBinding(name, provider(target, method)));
        }
        return List.copyOf(suggestions);
    }

    private static void validateSuggestionProvider(Method method) {
        boolean validParameter = method.getParameterCount() == 0
            || (method.getParameterCount() == 1
            && (method.getParameterTypes()[0] == ArgumentParseContext.class
            || method.getParameterTypes()[0] == SuggestionContext.class));
        boolean validReturn = List.class.isAssignableFrom(method.getReturnType())
            || method.getReturnType() == SuggestionSet.class;
        if (method.getParameterCount() > 1
            || !validParameter
            || !validReturn) {
            throw new IllegalArgumentException("@Suggest provider must return List or SuggestionSet and accept zero args, ArgumentParseContext, or SuggestionContext: "
                + method.getName());
        }
    }

    private static SuggestionProvider provider(Object target, Method method) {
        return new SuggestionProvider() {
            @Override
            public List<String> suggestions(ArgumentParseContext context) {
                return richSuggestions(context).stream()
                    .map(Suggestion::value)
                    .toList();
            }

            @Override
            public List<Suggestion> richSuggestions(ArgumentParseContext context) {
                return invokeProvider(target, method, context);
            }
        };
    }

    private static List<Suggestion> invokeProvider(Object target, Method method, ArgumentParseContext context) {
        if (target == null) {
            return List.of();
        }
        try {
            Object value = method.getParameterCount() == 0 ? method.invoke(target) : method.invoke(target,
                providerArgument(method, context));
            if (value instanceof SuggestionSet set) {
                return set.toSuggestions(SuggestionContext.from(context));
            }
            if (value instanceof List<?> list) {
                return suggestions(method.getName(), list, context);
            }
            throw new IllegalStateException("suggestion provider did not return a List or SuggestionSet: "
                + method.getName());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot invoke suggestion provider: " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("suggestion provider failed: " + method.getName(), cause);
        }
    }

    private static Object providerArgument(Method method, ArgumentParseContext context) {
        if (method.getParameterTypes()[0] == SuggestionContext.class) {
            return SuggestionContext.from(context);
        }
        return context;
    }

    private static List<Suggestion> suggestions(String providerName, List<?> values, ArgumentParseContext context) {
        List<Suggestion> suggestions = new ArrayList<>();
        Class<?> elementType = null;
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value == null) {
                throw new IllegalStateException("suggestion provider " + providerName
                    + " returned null at index " + index);
            }
            if (elementType == null) {
                elementType = value.getClass();
            } else if (elementType != value.getClass()) {
                throw new IllegalStateException("suggestion provider " + providerName
                    + " returned mixed element at index " + index);
            }
            if (value instanceof Suggestion suggestion) {
                suggestions.add(suggestion);
            } else if (value instanceof String suggestion) {
                suggestions.add(new Suggestion(
                    suggestion,
                    Optional.empty(),
                    context.replacementStart(),
                    context.replacementEnd(),
                    context.suggestionType(),
                    0
                ));
            } else {
                throw new IllegalStateException("suggestion provider must return List<String> or List<Suggestion>: "
                    + providerName);
            }
        }
        return List.copyOf(suggestions);
    }

    private static List<CommandMiddleware> annotatedMiddlewares(Class<?> owner, Method method) {
        List<CommandMiddleware> middlewares = new ArrayList<>();
        if (!owner.isAnnotationPresent(dev.riege.buildmycommand.annotation.Command.class)
            && !owner.isAnnotationPresent(dev.riege.buildmycommand.annotation.Subcommand.class)) {
            middlewares.addAll(annotatedMiddlewares(owner));
        }
        middlewares.addAll(annotatedMiddlewares(method));
        return List.copyOf(middlewares);
    }

    static List<CommandMiddleware> annotatedMiddlewares(AnnotatedElement element) {
        Middleware annotation = element.getAnnotation(Middleware.class);
        if (annotation == null) {
            return List.of();
        }
        List<CommandMiddleware> middlewares = new ArrayList<>();
        for (Class<? extends CommandMiddleware> type : annotation.value()) {
            middlewares.add(instantiateMiddleware(type));
        }
        return List.copyOf(middlewares);
    }

    private static CommandMiddleware instantiateMiddleware(Class<? extends CommandMiddleware> type) {
        try {
            Constructor<? extends CommandMiddleware> constructor = type.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException("command middleware must declare a no-arg constructor: "
                + type.getName(), exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("cannot instantiate command middleware: " + type.getName(), exception);
        }
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
        Optional<String> requirement,
        boolean suggestAliases,
        List<SuggestionBinding> suggestions,
        List<CommandMiddleware> middlewares
    ) {
        public CommandMetadata {
            usage = Objects.requireNonNull(usage, "usage");
            examples = List.copyOf(Objects.requireNonNull(examples, "examples"));
            cooldown = Objects.requireNonNull(cooldown, "cooldown");
            requirement = Objects.requireNonNull(requirement, "requirement");
            suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions"));
            middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
        }
    }

    public record ParameterBinding(Kind kind) {
        static ParameterBinding context() {
            return new ParameterBinding(Kind.CONTEXT);
        }

        static ParameterBinding routeContext() {
            return new ParameterBinding(Kind.ROUTE_CONTEXT);
        }

        Object value(CommandContext context) {
            return context;
        }
    }

    public record SuggestionBinding(String name, SuggestionProvider provider) {
        public SuggestionBinding {
            name = metadata(name, "suggestion provider");
            Objects.requireNonNull(provider, "provider");
        }
    }

    public enum Kind {
        CONTEXT,
        ROUTE_CONTEXT
    }
}
