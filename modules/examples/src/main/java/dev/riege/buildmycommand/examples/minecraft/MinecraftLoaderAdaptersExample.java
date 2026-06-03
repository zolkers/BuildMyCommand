package dev.riege.buildmycommand.examples.minecraft;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.minecraft.fabric.FabricBrigadierRegistration;
import dev.riege.buildmycommand.adapters.minecraft.fabric.FabricMinecraftAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Optional;

public final class MinecraftLoaderAdaptersExample {
    private MinecraftLoaderAdaptersExample() {
    }

    public static CommandFramework framework() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("moderation|mod reload <target:String>")
            .permission("mod.reload")
            .executes(ctx -> Results.success("Reloaded " + ctx.arg("target", String.class)));
        return framework;
    }

    public static FabricBrigadierRegistration<NativeCommandSource> fabricLegacyRegistration() {
        return FabricMinecraftAdapter.legacyRegistration(framework(), MinecraftLoaderAdaptersExample::source);
    }

    public static FabricBrigadierRegistration<NativeCommandSource> fabricModernRegistration() {
        return FabricMinecraftAdapter.registration(framework(), MinecraftLoaderAdaptersExample::source);
    }

    public static void registerFabric1165(CommandDispatcher<NativeCommandSource> dispatcher) {
        fabricLegacyRegistration().registerInto(dispatcher);
    }

    public static void registerFabricModern(CommandDispatcher<NativeCommandSource> dispatcher) {
        fabricModernRegistration().registerInto(dispatcher);
    }

    private static CommandSource source(NativeCommandSource nativeSource) {
        return new CommandSource() {
            @Override
            public Optional<String> name() {
                return Optional.of(nativeSource.name());
            }

            @Override
            public boolean hasPermission(String permission) {
                return nativeSource.hasPermission(permission);
            }
        };
    }

    public static final class NativeCommandSource {
        private final String name;
        private final boolean operator;

        public NativeCommandSource(String name, boolean operator) {
            this.name = name;
            this.operator = operator;
        }

        public String name() {
            return name;
        }

        public boolean hasPermission(String permission) {
            return operator || permission.isBlank();
        }
    }
}
