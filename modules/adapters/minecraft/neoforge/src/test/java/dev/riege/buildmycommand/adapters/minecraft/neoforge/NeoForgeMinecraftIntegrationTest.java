package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeoForgeMinecraftIntegrationTest {
    @Test
    void createsRegistrationForNeoForgeRegisterCommandsEvent() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        framework.registry()
            .route("neoforge|nfg sync <target:String>")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class));
                return Results.success("ok");
            });

        NeoForgeBrigadierRegistration<NeoForgeSource> registration =
            NeoForgeMinecraftIntegration.registration(framework, NeoForgeSource::commandSource);
        CommandDispatcher<NeoForgeSource> dispatcher = new CommandDispatcher<>();

        registration.registerInto(dispatcher);

        assertEquals("neoforge", NeoForgeMinecraftIntegration.profile().id());
        assertEquals("net.neoforged.neoforge.event.RegisterCommandsEvent",
            registration.api().eventClassName());
        assertEquals("NeoForge RegisterCommandsEvent", registration.api().displayName());
        assertEquals("neoforge", registration.plan().backend().id());
        assertEquals(2, registration.plan().rootLabels().size());
        assertEquals(1, dispatcher.execute("nfg sync Ada", new NeoForgeSource("Ada")));
        assertEquals("Ada", executed.get());
    }

    @Test
    void constructorIsNotPublic() throws Exception {
        Constructor<NeoForgeMinecraftIntegration> constructor = NeoForgeMinecraftIntegration.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException exception) {
            throw exception;
        }
    }

    private record NeoForgeSource(String name) {
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
