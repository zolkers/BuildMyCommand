package dev.riege.buildmycommand.adapters.minecraft.forge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

public final class ForgeMinecraftAdapter {
    private ForgeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.forge();
    }

    public static MinecraftInvocation brigadierInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }
}
