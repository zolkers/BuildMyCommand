package dev.riege.buildmycommand.core.registry;


import dev.riege.buildmycommand.core.route.*;
import dev.riege.buildmycommand.core.support.Validators;
import java.util.List;
import java.util.Objects;

public record RegistryCommandPath(List<String> literals, List<RegistryCommandNode> nodes) {
    public RegistryCommandPath {
        literals = List.copyOf(Objects.requireNonNull(literals, "literals"));
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes must not be empty");
        }
    }

    public RegistryCommandNode node() {
        return nodes.get(nodes.size() - 1);
    }
}
