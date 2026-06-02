package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteOptionKind;
import dev.riege.buildmycommand.dsl.RouteParser;
import dev.riege.buildmycommand.dsl.RoutePattern;
import dev.riege.buildmycommand.dsl.RouteStep;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
                && !routeNames.arguments().contains(binding.name())) {
                throw missingBinding(routeNames, binding, "Arg");
            }
            if ((binding.kind() == MethodCommandBinder.Kind.OPTION
                || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION)
                && !routeNames.valueOptions().contains(binding.name())) {
                throw missingBinding(routeNames, binding, "Option");
            }
            if (binding.kind() == MethodCommandBinder.Kind.FLAG
                && !routeNames.flags().contains(binding.name())) {
                throw missingBinding(routeNames, binding, "Flag");
            }
        }
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
        for (String argument : routeNames.arguments()) {
            if (bindings.stream().noneMatch(binding -> binding.kind() == MethodCommandBinder.Kind.ARGUMENT
                && argument.equals(binding.name()))) {
                throw new IllegalArgumentException("route argument " + argument
                    + " has no matching method parameter on " + method.getName());
            }
        }
        for (String option : routeNames.valueOptions()) {
            if (bindings.stream().noneMatch(binding -> (binding.kind() == MethodCommandBinder.Kind.OPTION
                || binding.kind() == MethodCommandBinder.Kind.OPTIONAL_OPTION) && option.equals(binding.name()))) {
                throw new IllegalArgumentException("route option " + option
                    + " has no matching method parameter on " + method.getName());
            }
        }
        for (String flag : routeNames.flags()) {
            if (bindings.stream().noneMatch(binding -> binding.kind() == MethodCommandBinder.Kind.FLAG
                && flag.equals(binding.name()))) {
                throw new IllegalArgumentException("route flag " + flag
                    + " has no matching method parameter on " + method.getName());
            }
        }
    }

    private static RouteNames routeNames(String route) {
        RoutePattern pattern = RouteParser.parse(route);
        Set<String> arguments = new LinkedHashSet<>();
        Set<String> valueOptions = new LinkedHashSet<>();
        Set<String> flags = new LinkedHashSet<>();
        for (RouteStep step : pattern.steps()) {
            if (step instanceof ArgumentRouteStep argument) {
                arguments.add(argument.name());
            } else if (step instanceof OptionRouteStep option) {
                if (option.kind() == RouteOptionKind.FLAG) {
                    flags.add(option.name());
                } else {
                    valueOptions.add(option.name());
                }
            }
        }
        return new RouteNames(pattern.rootLiteral(), arguments, valueOptions, flags);
    }

    private record RouteNames(String rootLiteral, Set<String> arguments, Set<String> valueOptions, Set<String> flags) {
    }
}
