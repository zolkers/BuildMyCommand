package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.riege.buildmycommand.dsl.ArgumentRouteStep;
import dev.riege.buildmycommand.dsl.LiteralRouteStep;
import dev.riege.buildmycommand.dsl.OptionRouteStep;
import dev.riege.buildmycommand.dsl.RouteArgumentKind;
import dev.riege.buildmycommand.dsl.RouteCanonicalizer;
import dev.riege.buildmycommand.dsl.RouteConflict;
import dev.riege.buildmycommand.dsl.RouteConflictAnalyzer;
import dev.riege.buildmycommand.dsl.RouteOptionKind;
import dev.riege.buildmycommand.dsl.RoutePattern;
import dev.riege.buildmycommand.dsl.RouteStep;
import dev.riege.buildmycommand.dsl.RouteType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandSchemaExporter {
    public String exportJson(CommandFramework framework) {
        return exportJson(Objects.requireNonNull(framework, "framework").graph());
    }

    public String exportJson(CommandGraph graph) {
        Objects.requireNonNull(graph, "graph");
        StringBuilder json = new StringBuilder();
        json.append("{\"commands\":[");
        List<CommandNode> nodes = flatten(graph);
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendNode(json, nodes.get(i), pathFor(graph, nodes.get(i)));
        }
        json.append("]}");
        return json.toString();
    }

    public String exportMermaid(CommandFramework framework) {
        CommandGraph graph = Objects.requireNonNull(framework, "framework").graph();
        StringBuilder builder = new StringBuilder("graph TD\n");
        for (CommandNode root : graph.roots()) {
            appendMermaid(builder, null, root, root.literal());
        }
        return builder.toString();
    }

    public ConflictReport detectConflicts(CommandFramework framework) {
        return detectConflicts(Objects.requireNonNull(framework, "framework").graph());
    }

    public ConflictReport detectConflicts(CommandGraph graph) {
        Objects.requireNonNull(graph, "graph");
        List<RouteConflict> conflicts = RouteConflictAnalyzer.findConflicts(executableRoutes(graph));
        return new ConflictReport(conflicts.stream()
            .map(conflict -> RouteCanonicalizer.canonical(conflict.first()) + " conflicts with "
                + RouteCanonicalizer.canonical(conflict.second()))
            .toList());
    }

    private static List<CommandNode> flatten(CommandGraph graph) {
        List<CommandNode> nodes = new ArrayList<>();
        graph.roots().forEach(root -> collect(root, nodes));
        return nodes;
    }

    private static void collect(CommandNode node, List<CommandNode> nodes) {
        nodes.add(node);
        node.children().forEach(child -> collect(child, nodes));
    }

    private static List<RoutePattern> executableRoutes(CommandGraph graph) {
        List<RoutePattern> routes = new ArrayList<>();
        for (CommandNode root : graph.roots()) {
            collectRoutes(root, root.literal(), root.aliases(), List.of(), routes);
        }
        return List.copyOf(routes);
    }

    private static void collectRoutes(
        CommandNode node,
        String rootLiteral,
        List<String> rootAliases,
        List<RouteStep> prefix,
        List<RoutePattern> routes
    ) {
        if (node.executor().isPresent()) {
            List<RouteStep> steps = new ArrayList<>(prefix);
            node.arguments().forEach(argument -> steps.add(argumentStep(argument)));
            node.flags().forEach(option -> steps.add(optionStep(option)));
            routes.add(new RoutePattern(rootLiteral, rootAliases, steps));
        }
        for (CommandNode child : node.children()) {
            List<RouteStep> childPrefix = new ArrayList<>(prefix);
            childPrefix.add(new LiteralRouteStep(child.literal(), child.aliases()));
            collectRoutes(child, rootLiteral, rootAliases, childPrefix, routes);
        }
    }

    private static ArgumentRouteStep argumentStep(ArgumentSpec<?> argument) {
        return new ArgumentRouteStep(
            argument.name(),
            routeType(argument.type()),
            switch (argument.kind()) {
                case REQUIRED -> RouteArgumentKind.REQUIRED;
                case OPTIONAL -> RouteArgumentKind.OPTIONAL;
                case GREEDY -> RouteArgumentKind.GREEDY;
                case OPTIONAL_GREEDY -> RouteArgumentKind.OPTIONAL_GREEDY;
            }
        );
    }

    private static OptionRouteStep optionStep(FlagSpec<?> option) {
        return new OptionRouteStep(
            option.name(),
            routeType(option.type()),
            option.alias(),
            option.kind() == FlagSpec.Kind.FLAG ? RouteOptionKind.FLAG : RouteOptionKind.VALUE
        );
    }

    private static RouteType routeType(Class<?> type) {
        return RouteType.runtime(type.getSimpleName(), type);
    }

    private static List<String> pathFor(CommandGraph graph, CommandNode target) {
        for (CommandNode root : graph.roots()) {
            List<String> path = findPath(root, target, new ArrayList<>());
            if (!path.isEmpty()) {
                return path;
            }
        }
        return List.of(target.literal());
    }

    private static List<String> findPath(CommandNode current, CommandNode target, List<String> prefix) {
        List<String> path = new ArrayList<>(prefix);
        path.add(current.literal());
        if (current == target) {
            return path;
        }
        for (CommandNode child : current.children()) {
            List<String> found = findPath(child, target, path);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return List.of();
    }

    private static void appendNode(StringBuilder json, CommandNode node, List<String> path) {
        json.append('{');
        property(json, "path", String.join(" ", path));
        json.append(",\"aliases\":");
        stringArray(json, node.aliases());
        json.append(",\"arguments\":");
        arguments(json, node.arguments());
        json.append(",\"options\":");
        options(json, node.flags());
        node.description().ifPresent(value -> optional(json, "description", value));
        node.permission().ifPresent(value -> optional(json, "permission", value));
        metadata(json, node.metadata());
        json.append('}');
    }

    private static void arguments(StringBuilder json, List<ArgumentSpec<?>> arguments) {
        json.append('[');
        for (int i = 0; i < arguments.size(); i++) {
            ArgumentSpec<?> argument = arguments.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{');
            property(json, "name", argument.name());
            optional(json, "type", argument.type().getSimpleName());
            optional(json, "kind", argument.kind().name());
            json.append('}');
        }
        json.append(']');
    }

    private static void options(StringBuilder json, List<FlagSpec<?>> options) {
        json.append('[');
        for (int i = 0; i < options.size(); i++) {
            FlagSpec<?> option = options.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{');
            property(json, "name", option.name());
            optional(json, "type", option.type().getSimpleName());
            optional(json, "kind", option.kind().name());
            option.aliasOptional().ifPresent(alias -> optional(json, "alias", alias));
            json.append('}');
        }
        json.append(']');
    }

    private static void metadata(StringBuilder json, CommandMetadata metadata) {
        json.append(",\"hidden\":").append(metadata.hidden());
        metadata.usage().ifPresent(value -> optional(json, "usage", value));
        json.append(",\"examples\":");
        stringArray(json, metadata.examples());
        metadata.cooldown().ifPresent(value -> optional(json, "cooldown", value.toString()));
        metadata.requirement().ifPresent(value -> optional(json, "require", value));
        metadata.group().ifPresent(value -> optional(json, "group", value));
    }

    private static void appendMermaid(StringBuilder builder, String parentId, CommandNode node, String path) {
        String id = sanitize(path);
        builder.append("  ").append(id).append("[\"").append(escape(path)).append("\"]\n");
        if (parentId != null) {
            builder.append("  ").append(parentId).append(" --> ").append(id).append('\n');
        }
        for (CommandNode child : node.children()) {
            appendMermaid(builder, id, child, path + " " + child.literal());
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9]+", "_");
    }

    private static void property(StringBuilder json, String name, String value) {
        json.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    }

    private static void optional(StringBuilder json, String name, String value) {
        json.append(',');
        property(json, name, value);
    }

    private static void stringArray(StringBuilder json, List<String> values) {
        json.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(escape(values.get(i))).append('"');
        }
        json.append(']');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
