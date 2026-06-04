/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.sponge;

public interface SpongeCommandRegistrar<C> {
    void register(Object pluginContainer, C command, String alias, String[] aliases);
}
