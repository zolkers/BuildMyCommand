package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeMinecraftAdapterTest {
    @Test
    void exposesNeoForgeRegisterCommandsEventProfile() {
        assertTrue(NeoForgeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
    }

    @Test
    void createsNeoForgeBrigadierBridgeFromFrameworkGraph() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("neoforge", command -> command.executes(ctx -> Results.success("ok")));

        var bridge = NeoForgeMinecraftAdapter.brigadierBridge(framework, NativeSource::source);

        assertEquals("neoforge", bridge.roots().get(0).getName());
        assertEquals("neoforge", MinecraftCommandRegistrationPlans.from(NeoForgeMinecraftAdapter.profile(), bridge)
            .rootLiterals().get(0));
    }

    @Test
    void registersNeoForgeEventDispatcherRootsAndAliases() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("neoforge|nfg reload").executes(ctx -> Results.silent());
        NeoForgeCommandRegistration<NativeSource> registration =
            NeoForgeMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(List.of("neoforge", "nfg"), registration.labels());
        assertEquals(List.of("neoforge", "nfg"), registration.register(dispatcher).stream().toList());
        assertNotNull(dispatcher.getRoot().getChild("neoforge"));
        assertNotNull(dispatcher.getRoot().getChild("nfg"));
        assertEquals("RegisterCommandsEvent", registration.eventName());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "NeoForge RegisterCommandsEvent exposes the Minecraft Brigadier dispatcher; aliases are exact redirect literals.",
            registration.matchingNotice()
        );
    }

    @Test
    void exposesNeoForgeRegisterCommandsEventCompatibleHandler() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("neoforge|nfg reload").executes(ctx -> Results.silent());
        NeoForgeCommandRegistration<NativeSource> registration =
            NeoForgeMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(List.of("neoforge", "nfg"), registration.register(() -> dispatcher).stream().toList());
        assertNotNull(dispatcher.getRoot().getChild("neoforge"));
        assertNotNull(dispatcher.getRoot().getChild("nfg"));
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
