package dev.riege.buildmycommand.examples.minecraft;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Set;

public final class MinecraftBrigadierExample {
    private MinecraftBrigadierExample() {
    }

    public static BrigadierCommandAdapter<FakeStack> createForFabricForgeNeoForgeStyleDispatchers() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("worldborder|wb set <size:Integer>")
            .permission("minecraft.command.worldborder")
            .executes(ctx -> Results.success("Border -> " + ctx.arg("size", Integer.class)));
        return MinecraftBrigadierAdapters.create(
            MinecraftBackendProfiles.fabric(),
            framework,
            stack -> new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return stack.permissions().contains(permission);
                }
            }
        );
    }

    public record FakeStack(Set<String> permissions) {
    }
}
