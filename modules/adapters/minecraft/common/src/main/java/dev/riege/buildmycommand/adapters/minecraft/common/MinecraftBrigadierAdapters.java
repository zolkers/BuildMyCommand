/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Objects;
import java.util.function.Function;

public final class MinecraftBrigadierAdapters {
    private static final CommandPlatform PLATFORM = new CommandPlatform("minecraft", "Minecraft", false, true, true);

    private MinecraftBrigadierAdapters() {
    }

    public static <N> BrigadierCommandAdapter<N> create(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return create(MinecraftBackendProfiles.fabric(), framework, sourceMapper);
    }

    public static <N> BrigadierCommandAdapter<N> create(
        MinecraftBackendProfile backend,
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        Objects.requireNonNull(backend, "backend");
        return BrigadierCommandAdapter.create(
            framework,
            sourceMapper,
            PLATFORM,
            new AdapterConfig(
                "minecraft-" + backend.id() + "-brigadier",
                backend.displayName() + " Brigadier",
                AdapterCapabilities.from(PLATFORM)
            )
        );
    }
}
