package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierBridge;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.function.Function;

public final class NeoForgeMinecraftAdapter {
    private NeoForgeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.neoforge();
    }

    public static MinecraftInvocation brigadierInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }

    public static <N> MinecraftBrigadierBridge<N> brigadierBridge(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return MinecraftBrigadierBridge.create(framework, sourceMapper);
    }
}
