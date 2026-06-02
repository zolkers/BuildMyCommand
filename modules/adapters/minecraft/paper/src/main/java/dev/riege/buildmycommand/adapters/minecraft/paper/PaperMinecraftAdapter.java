package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.core.CommandFramework;

public final class PaperMinecraftAdapter {
    private PaperMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.paper();
    }

    public static MinecraftInvocation brigadierInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }

    public static MinecraftInvocation commandInput(String label, String[] args) {
        return MinecraftInvocation.labelAndArgs(label, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> commandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }
}
