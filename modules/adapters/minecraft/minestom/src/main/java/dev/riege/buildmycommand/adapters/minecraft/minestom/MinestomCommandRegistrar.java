/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.minestom;

public interface MinestomCommandRegistrar {
    void register(MinestomNativeCommand command);

    void unregister(MinestomNativeCommand command);
}
