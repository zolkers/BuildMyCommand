package dev.riege.buildmycommand.dsl;

public record RouteConflict(RoutePattern first, RoutePattern second, String canonicalRoute) {
}
