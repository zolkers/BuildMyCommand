package dev.riege.buildmycommand.adapters.brigadier;

import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record BrigadierRoot<N>(
    LiteralCommandNode<N> root,
    List<String> aliases,
    List<LiteralCommandNode<N>> aliasRoots
) {
    public BrigadierRoot {
        Objects.requireNonNull(root, "root");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        aliasRoots = List.copyOf(Objects.requireNonNull(aliasRoots, "aliasRoots"));
    }

    public List<String> registrationLabels() {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(root.getLiteral());
        labels.addAll(aliases);
        return List.copyOf(labels);
    }
}
