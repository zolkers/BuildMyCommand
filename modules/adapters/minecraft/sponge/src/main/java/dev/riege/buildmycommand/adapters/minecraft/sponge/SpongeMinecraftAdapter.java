package dev.riege.buildmycommand.adapters.minecraft.sponge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;

import java.util.List;

public final class SpongeMinecraftAdapter {
    private SpongeMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.sponge();
    }

    public static MinecraftInvocation commandInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }

    public static <C> SpongeCommandRegistration<C> registration(
        Object pluginContainer,
        C command,
        List<String> labels
    ) {
        return new SpongeCommandRegistration<>(profile(), pluginContainer, command, labels);
    }
}
