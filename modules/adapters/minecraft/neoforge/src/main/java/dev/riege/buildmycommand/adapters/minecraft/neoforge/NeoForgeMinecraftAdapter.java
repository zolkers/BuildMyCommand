package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

public final class NeoForgeMinecraftAdapter {
    private NeoForgeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.neoforge();
    }

    public static MinecraftInvocation brigadierInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }
}
