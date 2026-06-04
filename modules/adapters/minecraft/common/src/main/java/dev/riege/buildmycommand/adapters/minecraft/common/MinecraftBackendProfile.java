/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Objects;
import java.util.Set;

public record MinecraftBackendProfile(
    String id,
    String displayName,
    Set<MinecraftCapability> capabilities,
    Set<MinecraftCommandEdgeCase> edgeCases,
    boolean reloadSafe
) {
    public MinecraftBackendProfile {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
        edgeCases = Set.copyOf(Objects.requireNonNull(edgeCases, "edgeCases"));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
