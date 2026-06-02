package dev.riege.buildmycommand.adapters.minecraft.fabric;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricMinecraftAdapterTest {
    @Test
    void exposesCommandRegistrationCallbackConstraints() {
        assertTrue(FabricMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.DEDICATED_ENVIRONMENT));
        assertEquals("fabric test", FabricMinecraftAdapter.brigadierInput("/fabric test", 12).normalizedInput());
    }

    @Test
    void createsFabricBrigadierBridgeFromFrameworkGraph() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("fabric", command -> command.executes(ctx -> Results.success("ok")));

        var bridge = FabricMinecraftAdapter.brigadierBridge(framework, NativeSource::source);

        assertEquals("fabric", bridge.roots().get(0).getName());
        assertEquals("fabric", bridge.registrationPlan(FabricMinecraftAdapter.profile()).rootLiterals().get(0));
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
