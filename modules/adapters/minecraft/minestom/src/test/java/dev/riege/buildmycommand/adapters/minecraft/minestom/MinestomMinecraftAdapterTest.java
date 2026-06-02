package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinestomMinecraftAdapterTest {
    @Test
    void exposesMinestomCommandManagerProfile() {
        assertTrue(MinestomMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertEquals("minestom reload", MinestomMinecraftAdapter.commandInput("minestom", new String[] {"reload"}).normalizedInput());
    }

    @Test
    void createsNativeCommandWithAliasesAndRegistrationPlan() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms reload").executes(ctx -> Results.silent());

        MinestomCommandRegistration registration = MinestomMinecraftAdapter.registration(
            MinestomMinecraftAdapter.commandAdapter(framework)
        );
        MinestomNativeCommand command = registration.command();

        assertEquals("minestom", command.getName());
        assertArrayEquals(new String[] {"ms"}, command.getAliases());
        assertEquals(java.util.List.of("minestom", "ms"), registration.labels());
        assertEquals(java.util.List.of("minestom", "ms"), registration.plan().rootLiterals());
        assertTrue(registration.exactLiteralMatching());
    }

    @Test
    void registrationDelegatesToMinestomRegistrarBoundary() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms reload").executes(ctx -> Results.silent());
        RecordingRegistrar registrar = new RecordingRegistrar();

        MinestomCommandRegistration registration = MinestomMinecraftAdapter.registration(
            MinestomMinecraftAdapter.commandAdapter(framework)
        );

        assertSame(registration.command(), registration.register(registrar));
        assertSame(registration.command(), registrar.registered());
        registration.unregister(registrar);
        assertSame(registration.command(), registrar.unregistered());
    }

    private static final class RecordingRegistrar implements MinestomCommandRegistrar {
        private MinestomNativeCommand registered;
        private MinestomNativeCommand unregistered;

        @Override
        public void register(MinestomNativeCommand command) {
            registered = command;
        }

        @Override
        public void unregister(MinestomNativeCommand command) {
            unregistered = command;
        }

        MinestomNativeCommand registered() {
            return registered;
        }

        MinestomNativeCommand unregistered() {
            return unregistered;
        }
    }
}
