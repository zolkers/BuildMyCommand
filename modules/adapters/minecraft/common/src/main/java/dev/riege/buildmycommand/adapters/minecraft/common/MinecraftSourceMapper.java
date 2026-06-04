/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandSource;

@FunctionalInterface
public interface MinecraftSourceMapper<S> {
    CommandSource map(S source);
}
