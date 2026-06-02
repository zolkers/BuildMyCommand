package dev.riege.buildmycommand.dsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RouteConflictAnalyzer {
    private RouteConflictAnalyzer() {
    }

    public static List<RouteConflict> findConflicts(Collection<RoutePattern> routes) {
        Map<String, RoutePattern> seen = new LinkedHashMap<>();
        List<RouteConflict> conflicts = new ArrayList<>();
        for (RoutePattern route : routes) {
            String canonical = RouteCanonicalizer.canonical(route);
            RoutePattern previous = seen.putIfAbsent(canonical, route);
            if (previous != null) {
                conflicts.add(new RouteConflict(previous, route, canonical));
            }
        }
        return List.copyOf(conflicts);
    }
}
