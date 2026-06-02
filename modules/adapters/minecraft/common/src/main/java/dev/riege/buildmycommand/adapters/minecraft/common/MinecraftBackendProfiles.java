package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Set;

public final class MinecraftBackendProfiles {
    private MinecraftBackendProfiles() {
    }

    public static MinecraftBackendProfile paper() {
        return new MinecraftBackendProfile(
            "paper",
            "Paper Brigadier Lifecycle",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.LIFECYCLE_EVENTS, MinecraftCapability.TAB_COMPLETION),
            Set.of(
                MinecraftCommandEdgeCase.SLASH_PREFIX,
                MinecraftCommandEdgeCase.BRIGADIER_CURSOR,
                MinecraftCommandEdgeCase.LIFECYCLE_REREGISTRATION,
                MinecraftCommandEdgeCase.PERMISSION_FILTERING
            ),
            true
        );
    }

    public static MinecraftBackendProfile spigot() {
        return new MinecraftBackendProfile(
            "spigot",
            "Bukkit/Spigot CommandExecutor",
            Set.of(MinecraftCapability.LEGACY_COMMAND_MAP, MinecraftCapability.TAB_COMPLETION),
            Set.of(
                MinecraftCommandEdgeCase.SLASH_PREFIX,
                MinecraftCommandEdgeCase.ARGS_ARRAY,
                MinecraftCommandEdgeCase.PERMISSION_FILTERING,
                MinecraftCommandEdgeCase.LEGACY_ALIAS_LABEL
            ),
            false
        );
    }

    public static MinecraftBackendProfile bungee() {
        return new MinecraftBackendProfile(
            "bungee",
            "BungeeCord Proxy Command",
            Set.of(MinecraftCapability.PROXY_COMMANDS, MinecraftCapability.TAB_COMPLETION),
            Set.of(
                MinecraftCommandEdgeCase.ARGS_ARRAY,
                MinecraftCommandEdgeCase.BUNGEE_TAB_COMPLETE,
                MinecraftCommandEdgeCase.PROXY_COMMANDS,
                MinecraftCommandEdgeCase.LEGACY_ALIAS_LABEL
            ),
            false
        );
    }

    public static MinecraftBackendProfile velocity() {
        return new MinecraftBackendProfile(
            "velocity",
            "Velocity Command API",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.PROXY_COMMANDS, MinecraftCapability.TAB_COMPLETION),
            Set.of(
                MinecraftCommandEdgeCase.BRIGADIER_CURSOR,
                MinecraftCommandEdgeCase.PROXY_COMMANDS,
                MinecraftCommandEdgeCase.PERMISSION_FILTERING,
                MinecraftCommandEdgeCase.LEGACY_ALIAS_LABEL
            ),
            true
        );
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

    public static MinecraftBackendProfile forge() {
        return new MinecraftBackendProfile(
            "forge",
            "Forge RegisterCommandsEvent",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.EVENT_BUS, MinecraftCapability.TAB_COMPLETION),
            Set.of(MinecraftCommandEdgeCase.BRIGADIER_CURSOR, MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION),
            true
        );
    }

    public static MinecraftBackendProfile neoforge() {
        return new MinecraftBackendProfile(
            "neoforge",
            "NeoForge RegisterCommandsEvent",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.EVENT_BUS, MinecraftCapability.TAB_COMPLETION),
            Set.of(MinecraftCommandEdgeCase.BRIGADIER_CURSOR, MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION),
            true
        );
    }
}
