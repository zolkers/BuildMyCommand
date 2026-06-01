package dev.riege.buildmycommand.core;

import java.util.List;
import java.util.Objects;

record RegistryCommandPath(List<String> literals, List<RegistryCommandNode> nodes) {
    RegistryCommandPath {
        literals = List.copyOf(Objects.requireNonNull(literals, "literals"));
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes must not be empty");
        }
    }

    RegistryCommandNode node() {
        return nodes.get(nodes.size() - 1);
    }
}
