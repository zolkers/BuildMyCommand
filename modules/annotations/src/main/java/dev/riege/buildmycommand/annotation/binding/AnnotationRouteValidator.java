package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteParser;
import dev.riege.buildmycommand.dsl.RoutePattern;
import dev.riege.buildmycommand.dsl.RouteStep;
import dev.riege.buildmycommand.dsl.RouteType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public final class AnnotationRouteValidator {
    private AnnotationRouteValidator() {
    }

    public static void validateRouteContextUsage(
        String route,
        Method method,
        List<MethodCommandBinder.ParameterBinding> bindings
    ) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(bindings, "bindings");

        long routeContexts = bindings.stream()
            .filter(binding -> binding.kind() == MethodCommandBinder.Kind.ROUTE_CONTEXT)
            .count();
        long plainContexts = bindings.stream()
            .filter(binding -> binding.kind() == MethodCommandBinder.Kind.CONTEXT)
            .count();
        if (routeContexts != 1 || plainContexts != 0 || bindings.size() != 1) {
            throw new IllegalArgumentException("@Route method must declare exactly one @RouteCtx CommandContext parameter: "
                + method.getName());
        }
        validateRuntimeTypes(route);
    }

    private static void validateRuntimeTypes(String route) {
        RoutePattern pattern = RouteParser.parse(route);
        for (RouteStep step : pattern.steps()) {
            if (step instanceof ArgumentRouteStep argument) {
                runtimeType("route argument", argument.name(), argument.type());
            } else if (step instanceof OptionRouteStep option) {
                runtimeType("route option", option.name(), option.type());
            }
        }
    }

    private static Class<?> runtimeType(String label, String name, RouteType type) {
        if (type.inlineEnum() || type.constrained()) {
            throw new IllegalArgumentException(label + " " + name + " uses analysis-only type "
                + type.displayName());
        }
        return type.runtimeType();
    }
}
