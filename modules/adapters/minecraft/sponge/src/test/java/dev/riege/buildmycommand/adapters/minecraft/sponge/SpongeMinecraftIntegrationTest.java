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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpongeMinecraftIntegrationTest {
    @Test
    void exposesSpongeLifecycleRegistrationProfile() {
        assertTrue(SpongeMinecraftIntegration.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
        assertEquals("sponge reload", SpongeMinecraftIntegration.commandInput("/sponge reload", 14).normalizedInput());
    }

    @Test
    void registersParameterizedCommandThroughRegisterCommandEventBoundary() {
        Object container = new Object();
        Object command = new Object();
        SpongeCommandRegistration<Object> registration = SpongeMinecraftIntegration.registration(
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
        assertSame(container, registration.pluginContainer());
        assertSame(command, registration.command());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "Sponge RegisterCommandEvent aliases are registration aliases; Sponge and Brigadier literals remain exact-case.",
            registration.matchingNotice()
        );
        assertEquals(SpongeMinecraftIntegration.profile(), registration.plan().backend());
        assertEquals(List.of("sponge", "sp"), registration.plan().rootLabels());
        assertEquals(1, registration.plan().generation());
        assertTrue(registration.plan().reloadSafe());
    }

    @Test
    void exposesSharedBrigadierBridgeForSpongeCommandEvents() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("sponge|sp reload").executes(ctx -> Results.silent());

        BrigadierCommandAdapter<Object> bridge = SpongeMinecraftIntegration.brigadierBridge(
            framework,
            source -> new CommandSource() {
            }
        );

        assertEquals("minecraft-sponge-brigadier", bridge.config().adapterId());
        assertEquals(List.of("sponge", "sp"), bridge.registrationLabels().rootLabels());
    }

    @Test
    void rejectsInvalidRegistrationInputs() {
        Object container = new Object();
        Object command = new Object();

        assertThrows(NullPointerException.class, () -> new SpongeCommandRegistration<>(
            null,
            container,
            command,
            List.of("sponge")
        ));
        assertThrows(NullPointerException.class, () -> SpongeMinecraftIntegration.registration(null, command, List.of("sponge")));
        assertThrows(NullPointerException.class, () -> SpongeMinecraftIntegration.registration(container, null, List.of("sponge")));
        assertThrows(NullPointerException.class, () -> SpongeMinecraftIntegration.registration(container, command, null));
        assertThrows(IllegalArgumentException.class, () -> SpongeMinecraftIntegration.registration(container, command, List.of()));
        assertThrows(NullPointerException.class, () -> SpongeMinecraftIntegration.registration(container, command, List.of("sponge"))
            .register(null));
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
