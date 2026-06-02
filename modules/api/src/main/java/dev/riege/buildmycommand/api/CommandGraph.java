package dev.riege.buildmycommand.api;

import java.util.List;
import java.util.Objects;

public record CommandGraph(List<CommandNode> roots) {
    public CommandGraph {
        roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
    }
}
