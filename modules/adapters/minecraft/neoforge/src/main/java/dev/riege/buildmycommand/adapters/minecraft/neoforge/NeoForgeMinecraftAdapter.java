package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class NeoForgeMinecraftAdapter {
    private NeoForgeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.neoforge();
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper::map);
    }

    public static <N> NeoForgeBrigadierRegistration<N> registration(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return new NeoForgeBrigadierRegistration<>(
            profile(),
            NeoForgeCommandApi.NEOFORGE_MODERN,
            brigadierBridge(framework, sourceMapper)
        );
    }
}
