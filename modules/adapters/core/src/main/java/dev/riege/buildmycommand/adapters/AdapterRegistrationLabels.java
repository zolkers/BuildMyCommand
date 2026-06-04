/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public record AdapterRegistrationLabels(
    List<String> rootLiterals,
    List<String> rootLabels
) {
    public AdapterRegistrationLabels {
        rootLiterals = List.copyOf(Objects.requireNonNull(rootLiterals, "rootLiterals"));
        rootLabels = List.copyOf(Objects.requireNonNull(rootLabels, "rootLabels"));
    }

    public static AdapterRegistrationLabels from(CommandFramework framework) {
        Objects.requireNonNull(framework, "framework");
        return new AdapterRegistrationLabels(framework.rootLiterals(), framework.rootLabels());
    }
}
