/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.fabric;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class FabricMinecraftIntegration {
    private FabricMinecraftIntegration() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.fabric();
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper::map);
    }

    public static <N> FabricBrigadierRegistration<N> registration(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return registration(FabricCommandApi.COMMAND_API_V2, framework, sourceMapper);
    }

    public static <N> FabricBrigadierRegistration<N> legacyRegistration(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return registration(FabricCommandApi.COMMAND_API_V1, framework, sourceMapper);
    }

    public static <N> FabricBrigadierRegistration<N> registration(
        FabricCommandApi api,
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return new FabricBrigadierRegistration<>(profile(), api, brigadierBridge(framework, sourceMapper));
    }
}
