package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

public final class SpigotMinecraftAdapter {
    private SpigotMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.spigot();
    }

    public static MinecraftInvocation commandExecutorInput(String label, String[] args) {
        return MinecraftInvocation.labelAndArgs(label, args, Math.max(0, args.length - 1));
    }
}
