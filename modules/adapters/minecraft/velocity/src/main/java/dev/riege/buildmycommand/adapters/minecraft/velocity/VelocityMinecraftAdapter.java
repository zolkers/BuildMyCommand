package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

public final class VelocityMinecraftAdapter {
    private VelocityMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.velocity();
    }

    public static MinecraftInvocation simpleCommandInput(String alias, String[] args) {
        return MinecraftInvocation.labelAndArgs(alias, args, Math.max(0, args.length - 1));
    }
}
