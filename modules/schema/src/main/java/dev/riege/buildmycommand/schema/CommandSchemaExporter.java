package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.core.CommandFramework;

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

    private static List<CommandNode> flatten(CommandGraph graph) {
        List<CommandNode> nodes = new ArrayList<>();
        graph.roots().forEach(root -> collect(root, nodes));
        return nodes;
    }

    private static void collect(CommandNode node, List<CommandNode> nodes) {
        nodes.add(node);
        node.children().forEach(child -> collect(child, nodes));
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
