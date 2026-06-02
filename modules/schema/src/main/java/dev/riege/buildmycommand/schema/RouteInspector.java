package dev.riege.buildmycommand.schema;

import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RouteInspector {
    public RouteInspection inspect(CommandFramework framework, String input) {
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(input, "input");
        List<String> tokens = List.of(input.trim().split("\\s+"));
        List<String> matched = new ArrayList<>();
        CommandNode current = null;
        for (String token : tokens) {
            CommandNode next = find(current == null ? framework.graph().roots() : current.children(), token);
            if (next == null) {
                break;
            }
            current = next;
            matched.add(next.literal());
        }
        return new RouteInspection(tokens, matched, current == null ? List.of() : current.arguments(),
            current == null ? List.of() : current.flags(), current != null && current.executor().isPresent());
    }

    private static CommandNode find(List<CommandNode> nodes, String token) {
        for (CommandNode node : nodes) {
            if (node.literal().equals(token) || node.aliases().contains(token)) {
                return node;
            }
        }
        return null;
    }
}
