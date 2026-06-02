package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class BungeeMinecraftAdapter {
    private BungeeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.bungee();
    }

    public static MinecraftInvocation commandInput(String boundLabel, String[] args) {
        return MinecraftInvocation.labelAndArgs(boundLabel, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> commandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }
}
