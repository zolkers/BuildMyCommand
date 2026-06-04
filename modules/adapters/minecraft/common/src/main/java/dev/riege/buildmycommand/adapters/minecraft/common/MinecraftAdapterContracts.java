/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MinecraftAdapterContracts {
    private MinecraftAdapterContracts() {
    }

    public static <S, R> List<String> rootLabels(IAdapter<S, MinecraftInvocation, R> adapter) {
        Objects.requireNonNull(adapter, "adapter");
        return adapter.registrationLabels().rootLabels();
    }

    public static <S, R> boolean canUseRootLabel(
        IAdapter<S, MinecraftInvocation, R> adapter,
        S source,
        String label
    ) {
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(label, "label");
        CommandSource mapped = adapter.mapSource(source);
        for (CommandNode root : adapter.runtime().framework().graph().roots()) {
            if (matchesLabel(adapter, root, label)) {
                return canAccessNode(mapped, root);
            }
        }
        return true;
    }

    private static <S, R> boolean matchesLabel(
        IAdapter<S, MinecraftInvocation, R> adapter,
        CommandNode root,
        String label
    ) {
        if (matches(adapter, root.literal(), label)) {
            return true;
        }
        for (String alias : root.aliases()) {
            if (matches(adapter, alias, label)) {
                return true;
            }
        }
        return false;
    }

    private static <S, R> boolean matches(IAdapter<S, MinecraftInvocation, R> adapter, String expected, String actual) {
        if (adapter.matchingPolicy().caseInsensitiveLiterals()) {
            return expected.toLowerCase(Locale.ROOT).equals(actual.toLowerCase(Locale.ROOT));
        }
        return expected.equals(actual);
    }

    private static boolean canAccessNode(CommandSource source, CommandNode node) {
        if (node.permission().isPresent() && !source.hasPermission(node.permission().get())) {
            return false;
        }
        if (node.executor().isPresent()) {
            return true;
        }
        for (CommandNode child : node.children()) {
            if (canAccessNode(source, child)) {
                return true;
            }
        }
        return node.children().isEmpty();
    }
}
