/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.sponge;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.function.Function;

public final class SpongeMinecraftIntegration {
    private SpongeMinecraftIntegration() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.sponge();
    }

    public static MinecraftInvocation commandInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper);
    }

    public static <C> SpongeCommandRegistration<C> registration(
        Object pluginContainer,
        C command,
        List<String> labels
    ) {
        return new SpongeCommandRegistration<>(profile(), pluginContainer, command, labels);
    }
}
