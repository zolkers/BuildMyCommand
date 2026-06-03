package dev.riege.buildmycommand.adapters.minecraft.forge;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeMinecraftAdapterTest {
    @Test
    void createsModernAndLegacyRegistrationsForForgeRegisterCommandsEvent() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        framework.registry()
            .route("forge|fg sync <target:String>")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class));
                return Results.success("ok");
            });

        ForgeBrigadierRegistration<ForgeSource> modern =
            ForgeMinecraftAdapter.registration(framework, ForgeSource::commandSource);
        ForgeBrigadierRegistration<ForgeSource> legacy =
            ForgeMinecraftAdapter.legacyRegistration(framework, ForgeSource::commandSource);
        CommandDispatcher<ForgeSource> dispatcher = new CommandDispatcher<>();

        modern.registerInto(dispatcher);

        assertEquals("forge", ForgeMinecraftAdapter.profile().id());
        assertEquals("net.minecraftforge.event.RegisterCommandsEvent", modern.api().eventClassName());
        assertEquals("Modern Forge RegisterCommandsEvent", modern.api().displayName());
        assertEquals("Forge 1.16.5 RegisterCommandsEvent", legacy.api().displayName());
        assertFalse(modern.legacyApi());
        assertTrue(legacy.legacyApi());
        assertEquals("forge", modern.plan().backend().id());
        assertEquals(2, modern.plan().rootLabels().size());
        assertEquals(1, dispatcher.execute("fg sync Ada", new ForgeSource("Ada")));
        assertEquals("Ada", executed.get());
    }

    @Test
    void constructorIsNotPublic() throws Exception {
        Constructor<ForgeMinecraftAdapter> constructor = ForgeMinecraftAdapter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException exception) {
            throw exception;
        }
    }

    private record ForgeSource(String name) {
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
