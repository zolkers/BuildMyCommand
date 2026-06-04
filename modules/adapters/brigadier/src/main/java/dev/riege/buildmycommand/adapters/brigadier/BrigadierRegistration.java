/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BrigadierRegistration<N> {
    private final BrigadierCommandAdapter<N> adapter;

    public BrigadierRegistration(BrigadierCommandAdapter<N> adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public BrigadierCommandAdapter<N> adapter() {
        return adapter;
    }

    public List<LiteralCommandNode<N>> roots() {
        return adapter.roots();
    }

    public List<BrigadierRoot<N>> projectedRoots() {
        return adapter.projectedRoots();
    }

    public List<String> labels() {
        return roots().stream().map(LiteralCommandNode::getLiteral).toList();
    }

    public Set<String> register(CommandDispatcher<N> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Set<String> registered = new LinkedHashSet<>();
        for (LiteralCommandNode<N> root : roots()) {
            if (dispatcher.getRoot().getChild(root.getLiteral()) != null) {
                continue;
            }
            dispatcher.getRoot().addChild(root);
            registered.add(root.getLiteral());
        }
        return Collections.unmodifiableSet(registered);
    }
}
