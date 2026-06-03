package dev.riege.buildmycommand.adapters.minecraft.sponge;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpongeMinecraftAdapterTest {
    @Test
    void exposesSpongeLifecycleRegistrationProfile() {
        assertTrue(SpongeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
        assertEquals("sponge reload", SpongeMinecraftAdapter.commandInput("/sponge reload", 14).normalizedInput());
    }

    @Test
    void registersParameterizedCommandThroughRegisterCommandEventBoundary() {
        Object container = new Object();
        Object command = new Object();
        SpongeCommandRegistration<Object> registration = SpongeMinecraftAdapter.registration(
            container,
            command,
            List.of("sponge", "sp")
        );
        RecordingRegistrar<Object> registrar = new RecordingRegistrar<>();

        registration.register(registrar);

        assertSame(container, registrar.container());
        assertSame(command, registrar.command());
        assertEquals("sponge", registrar.alias());
        assertEquals(List.of("sp"), Arrays.asList(registrar.aliases()));
        assertEquals(List.of("sponge", "sp"), registration.labels());
        assertTrue(registration.exactLiteralMatching());
    }

    @Test
    void exposesSharedBrigadierBridgeForSpongeCommandEvents() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("sponge|sp reload").executes(ctx -> Results.silent());

        BrigadierCommandAdapter<Object> bridge = SpongeMinecraftAdapter.brigadierBridge(
            framework,
            source -> new CommandSource() {
            }
        );

        assertEquals("minecraft-sponge-brigadier", bridge.config().adapterId());
        assertEquals(List.of("sponge", "sp"), bridge.registrationLabels().rootLabels());
    }

    private static final class RecordingRegistrar<C> implements SpongeCommandRegistrar<C> {
        private Object container;
        private C command;
        private String alias;
        private String[] aliases;

        @Override
        public void register(Object pluginContainer, C command, String alias, String[] aliases) {
            this.container = pluginContainer;
            this.command = command;
            this.alias = alias;
            this.aliases = aliases;
        }

        Object container() {
            return container;
        }

        C command() {
            return command;
        }

        String alias() {
            return alias;
        }

        String[] aliases() {
            return aliases;
        }
    }
}
