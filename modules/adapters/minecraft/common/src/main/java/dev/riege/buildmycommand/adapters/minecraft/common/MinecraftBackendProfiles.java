package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Set;

public final class MinecraftBackendProfiles {
    private MinecraftBackendProfiles() {
    }

    public static MinecraftBackendProfile fabric() {
        return new MinecraftBackendProfile(
            "fabric",
            "Fabric CommandRegistrationCallback",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.EVENT_BUS, MinecraftCapability.TAB_COMPLETION),
            Set.of(
                MinecraftCommandEdgeCase.BRIGADIER_CURSOR,
                MinecraftCommandEdgeCase.DEDICATED_ENVIRONMENT,
                MinecraftCommandEdgeCase.PERMISSION_FILTERING
            ),
            false
        );
    }
}
