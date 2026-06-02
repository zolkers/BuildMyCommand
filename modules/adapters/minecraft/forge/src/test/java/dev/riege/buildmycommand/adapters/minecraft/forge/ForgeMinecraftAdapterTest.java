package dev.riege.buildmycommand.adapters.minecraft.forge;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeMinecraftAdapterTest {
    @Test
    void exposesRegisterCommandsEventProfile() {
        assertTrue(ForgeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
    }

    @Test
    void createsForgeBrigadierBridgeFromFrameworkGraph() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("forge", command -> command.executes(ctx -> Results.success("ok")));

        var bridge = ForgeMinecraftAdapter.brigadierBridge(framework, NativeSource::source);

        assertEquals("forge", bridge.roots().get(0).getName());
        assertEquals("forge", bridge.registrationPlan(ForgeMinecraftAdapter.profile()).rootLiterals().get(0));
    }

    @Test
    void registersForgeEventDispatcherRootsAndAliases() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("forge|fg reload").executes(ctx -> Results.silent());
        ForgeCommandRegistration<NativeSource> registration =
            ForgeMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(List.of("forge", "fg"), registration.labels());
        assertEquals(List.of("forge", "fg"), registration.register(dispatcher).stream().toList());
        assertNotNull(dispatcher.getRoot().getChild("forge"));
        assertNotNull(dispatcher.getRoot().getChild("fg"));
        assertEquals("RegisterCommandsEvent", registration.eventName());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "Forge RegisterCommandsEvent exposes the Minecraft Brigadier dispatcher; aliases are exact redirect literals.",
            registration.matchingNotice()
        );
    }

    @Test
    void exposesForgeRegisterCommandsEventCompatibleHandler() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("forge|fg reload").executes(ctx -> Results.silent());
        ForgeCommandRegistration<NativeSource> registration =
            ForgeMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(List.of("forge", "fg"), registration.register(() -> dispatcher).stream().toList());
        assertNotNull(dispatcher.getRoot().getChild("forge"));
        assertNotNull(dispatcher.getRoot().getChild("fg"));
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
