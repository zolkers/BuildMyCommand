package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

public final class BungeeMinecraftAdapter {
    private BungeeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.bungee();
    }

    public static MinecraftInvocation commandInput(String boundLabel, String[] args) {
        return MinecraftInvocation.labelAndArgs(boundLabel, args, Math.max(0, args.length - 1));
    }
}
