package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
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

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(framework, sourceMapper);
    }

    public static <N> NeoForgeCommandRegistration<N> commandRegistration(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return new NeoForgeCommandRegistration<>(profile(), brigadierBridge(framework, sourceMapper));
    }
}
