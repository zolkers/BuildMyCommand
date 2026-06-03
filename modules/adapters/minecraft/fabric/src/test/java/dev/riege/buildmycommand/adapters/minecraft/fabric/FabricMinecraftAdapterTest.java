package dev.riege.buildmycommand.adapters.minecraft.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricMinecraftAdapterTest {
    @Test
    void createsModernAndLegacyRegistrationsForFabricCommandApis() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        framework.registry()
            .route("fabric|fb reload <target:String>")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class));
                return Results.success("ok");
            });

        FabricBrigadierRegistration<FabricSource> modern =
            FabricMinecraftAdapter.registration(framework, FabricSource::commandSource);
        FabricBrigadierRegistration<FabricSource> legacy =
            FabricMinecraftAdapter.legacyRegistration(framework, FabricSource::commandSource);
        CommandDispatcher<FabricSource> dispatcher = new CommandDispatcher<>();

        modern.registerInto(dispatcher);

        assertEquals("fabric", FabricMinecraftAdapter.profile().id());
        assertEquals("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback",
            modern.api().callbackClassName());
        assertEquals("Modern Fabric style", modern.api().displayName());
        assertEquals("net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback",
            legacy.api().callbackClassName());
        assertEquals("Minecraft 1.16.5 style", legacy.api().displayName());
        assertFalse(modern.legacyApi());
        assertTrue(legacy.legacyApi());
        assertEquals("fabric", modern.plan().backend().id());
        assertEquals(2, modern.plan().rootLabels().size());
        assertEquals(1, dispatcher.execute("fb reload Ada", new FabricSource("Ada")));
        assertEquals("Ada", executed.get());
    }

    @Test
    void registrationDoesNotExposeFallbackRootOrBreakExistingClientCommands() throws Exception {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("wecc ping")
            .executes(ctx -> Results.success("pong"));
        FabricBrigadierRegistration<FabricSource> registration =
            FabricMinecraftAdapter.registration(framework, FabricSource::commandSource);
        CommandDispatcher<FabricSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<FabricSource>literal("fabric-command-api-v2:client")
            .executes(context -> 37));

        registration.registerInto(dispatcher);

        assertNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertEquals(37, dispatcher.execute("fabric-command-api-v2:client", new FabricSource("Ada")));
        assertEquals(1, dispatcher.execute("wecc ping", new FabricSource("Ada")));
    }

    @Test
    void constructorIsNotPublic() throws Exception {
        Constructor<FabricMinecraftAdapter> constructor = FabricMinecraftAdapter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException exception) {
            throw exception;
        }
    }

    private record FabricSource(String name) {
        private CommandSource commandSource() {
            return new CommandSource() {
                @Override
                public Optional<String> name() {
                    return Optional.of(name);
                }
            };
        }
    }
}
