package dev.riege.buildmycommand.adapters.minecraft.forge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
