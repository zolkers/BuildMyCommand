package dev.riege.buildmycommand.adapters.minecraft.forge;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class ForgeMinecraftIntegration {
    private ForgeMinecraftIntegration() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.forge();
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper::map);
    }

    public static <N> ForgeBrigadierRegistration<N> registration(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return registration(ForgeCommandApi.FORGE_MODERN, framework, sourceMapper);
    }

    public static <N> ForgeBrigadierRegistration<N> legacyRegistration(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return registration(ForgeCommandApi.FORGE_1_16_5, framework, sourceMapper);
    }

    public static <N> ForgeBrigadierRegistration<N> registration(
        ForgeCommandApi api,
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return new ForgeBrigadierRegistration<>(profile(), api, brigadierBridge(framework, sourceMapper));
    }
}
