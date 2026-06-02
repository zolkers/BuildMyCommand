package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteArgumentKind;
import dev.riege.buildmycommand.dsl.RouteOptionKind;
import dev.riege.buildmycommand.dsl.RouteParser;
import dev.riege.buildmycommand.dsl.RoutePattern;
import dev.riege.buildmycommand.dsl.RouteStep;
import dev.riege.buildmycommand.dsl.RouteType;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashMap;

public final class AnnotationRouteValidator {
    private AnnotationRouteValidator() {
    }

    public static void validate(
        String route,
        Method method,
        List<MethodCommandBinder.ParameterBinding> bindings
    ) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(bindings, "bindings");

        RouteNames routeNames = routeNames(route);
        validateMethodBindings(bindings, routeNames);
        validateRouteCoverage(method, bindings, routeNames);
    }

    private static void validateMethodBindings(
        List<MethodCommandBinder.ParameterBinding> bindings,
        RouteNames routeNames
    ) {
        for (MethodCommandBinder.ParameterBinding binding : bindings) {
            if (binding.kind() == MethodCommandBinder.Kind.ARGUMENT
                && !routeNames.arguments().containsKey(binding.name())) {
                throw missingBinding(routeNames, binding, "Arg");
            }
            if ((binding.kind() == MethodCommandBinder.Kind.OPTION
                || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION)
                && !routeNames.valueOptions().containsKey(binding.name())) {
                throw missingBinding(routeNames, binding, "Option");
            }
            if (binding.kind() == MethodCommandBinder.Kind.FLAG
                && !routeNames.flags().containsKey(binding.name())) {
                throw missingBinding(routeNames, binding, "Flag");
            }
            validateCompatibleBinding(binding, routeNames);
        }
    }

    private static void validateCompatibleBinding(
        MethodCommandBinder.ParameterBinding binding,
        RouteNames routeNames
    ) {
        if (binding.kind() == MethodCommandBinder.Kind.ARGUMENT) {
            ArgumentDescriptor descriptor = routeNames.arguments().get(binding.name());
            if (descriptor == null) {
                return;
            }
            validateType("route argument", binding.name(), descriptor.type(), binding.type());
            validateArgumentKind(binding, descriptor);
        } else if (binding.kind() == MethodCommandBinder.Kind.OPTION
            || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) {
            OptionDescriptor descriptor = routeNames.valueOptions().get(binding.name());
            if (descriptor != null) {
                validateType("route option", binding.name(), descriptor.type(), binding.type());
            }
        }
    }

    private static void validateArgumentKind(
        MethodCommandBinder.ParameterBinding binding,
        ArgumentDescriptor descriptor
    ) {
        boolean routeOptional = descriptor.kind() == RouteArgumentKind.OPTIONAL
            || descriptor.kind() == RouteArgumentKind.OPTIONAL_GREEDY;
        boolean routeGreedy = descriptor.kind() == RouteArgumentKind.GREEDY
            || descriptor.kind() == RouteArgumentKind.OPTIONAL_GREEDY;
        if (routeOptional != binding.optional()) {
            throw new IllegalArgumentException("route argument " + binding.name()
                + (routeOptional ? " is optional but method parameter is not @OptionalArg"
                    : " is required but method parameter is @OptionalArg"));
        }
        if (routeGreedy != binding.greedy()) {
            throw new IllegalArgumentException("route argument " + binding.name()
                + (routeGreedy ? " is greedy but method parameter is not @Greedy"
                    : " is not greedy but method parameter is @Greedy"));
        }
    }

    private static void validateType(String label, String name, Class<?> routeType, Class<?> bindingType) {
        if (!boxed(routeType).equals(boxed(bindingType))) {
            throw new IllegalArgumentException(label + " " + name + " expects " + routeType.getSimpleName()
                + " but method parameter is " + bindingType.getSimpleName() + " on " + name);
        }
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        return type;
    }

    private static IllegalArgumentException missingBinding(
        RouteNames routeNames,
        MethodCommandBinder.ParameterBinding binding,
        String fallbackAnnotation
    ) {
        String annotationName = binding.annotationName() == null ? fallbackAnnotation : binding.annotationName();
        return new IllegalArgumentException("@" + annotationName + "(\"" + binding.name()
            + "\") does not exist in route DSL for " + routeNames.rootLiteral());
    }

    private static void validateRouteCoverage(
        Method method,
        List<MethodCommandBinder.ParameterBinding> bindings,
        RouteNames routeNames
    ) {
        for (String argument : routeNames.arguments().keySet()) {
            if (bindings.stream().noneMatch(binding -> binding.kind() == MethodCommandBinder.Kind.ARGUMENT
                && argument.equals(binding.name()))) {
                throw new IllegalArgumentException("route argument " + argument
                    + " has no matching method parameter on " + method.getName());
            }
        }
        for (String option : routeNames.valueOptions().keySet()) {
            if (bindings.stream().noneMatch(binding -> (binding.kind() == MethodCommandBinder.Kind.OPTION
                || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) && option.equals(binding.name()))) {
                throw new IllegalArgumentException("route option " + option
                    + " has no matching method parameter on " + method.getName());
            }
        }
        for (String flag : routeNames.flags().keySet()) {
            if (bindings.stream().noneMatch(binding -> binding.kind() == MethodCommandBinder.Kind.FLAG
                && flag.equals(binding.name()))) {
                throw new IllegalArgumentException("route flag " + flag
                    + " has no matching method parameter on " + method.getName());
            }
        }
    }

    private static RouteNames routeNames(String route) {
        RoutePattern pattern = RouteParser.parse(route);
        Map<String, ArgumentDescriptor> arguments = new LinkedHashMap<>();
        Map<String, OptionDescriptor> valueOptions = new LinkedHashMap<>();
        Map<String, OptionDescriptor> flags = new LinkedHashMap<>();
        for (RouteStep step : pattern.steps()) {
            if (step instanceof ArgumentRouteStep argument) {
                arguments.put(argument.name(), new ArgumentDescriptor(
                    runtimeType("route argument", argument.name(), argument.type()),
                    argument.kind()
                ));
            } else if (step instanceof OptionRouteStep option) {
                OptionDescriptor descriptor = new OptionDescriptor(
                    runtimeType("route option", option.name(), option.type()),
                    option.kind()
                );
                if (option.kind() == RouteOptionKind.FLAG) {
                    flags.put(option.name(), descriptor);
                } else {
                    valueOptions.put(option.name(), descriptor);
                }
            }
        }
        return new RouteNames(pattern.rootLiteral(), arguments, valueOptions, flags);
    }

    private static Class<?> runtimeType(String label, String name, RouteType type) {
        if (type.inlineEnum() || type.constrained()) {
            throw new IllegalArgumentException(label + " " + name + " uses analysis-only type "
                + type.displayName());
        }
        return type.runtimeType();
    }

    private record RouteNames(
        String rootLiteral,
        Map<String, ArgumentDescriptor> arguments,
        Map<String, OptionDescriptor> valueOptions,
        Map<String, OptionDescriptor> flags
    ) {
    }

    private record ArgumentDescriptor(Class<?> type, RouteArgumentKind kind) {
    }

    private record OptionDescriptor(Class<?> type, RouteOptionKind kind) {
    }
}
