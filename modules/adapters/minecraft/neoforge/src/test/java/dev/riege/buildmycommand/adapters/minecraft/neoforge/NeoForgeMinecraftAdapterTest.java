package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("neoforge", bridge.registrationPlan(NeoForgeMinecraftAdapter.profile()).rootLiterals().get(0));
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
