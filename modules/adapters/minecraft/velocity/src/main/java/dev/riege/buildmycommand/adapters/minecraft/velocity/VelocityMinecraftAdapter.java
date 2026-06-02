package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class VelocityMinecraftAdapter {
    private VelocityMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.velocity();
    }

    public static MinecraftInvocation simpleCommandInput(String alias, String[] args) {
        return MinecraftInvocation.labelAndArgs(alias, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> simpleCommandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }
}
