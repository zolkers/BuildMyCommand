package dev.riege.buildmycommand.adapters.minecraft.fabric;

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
        assertEquals("fabric", MinecraftCommandRegistrationPlans.from(FabricMinecraftAdapter.profile(), bridge)
            .rootLiterals().get(0));
    }

    @Test
    void registersFabricCallbackDispatcherRootsAndAliases() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("fabric|fb reload").executes(ctx -> Results.silent());
        FabricCommandRegistration<NativeSource> registration =
            FabricMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(List.of("fabric", "fb"), registration.labels());
        assertEquals(List.of("fabric", "fb"), registration.register(dispatcher).stream().toList());
        assertNotNull(dispatcher.getRoot().getChild("fabric"));
        assertNotNull(dispatcher.getRoot().getChild("fb"));
        assertEquals("CommandRegistrationCallback.EVENT", registration.callbackName());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "Fabric registers Brigadier literal nodes directly; aliases are redirect literals and remain exact-case.",
            registration.matchingNotice()
        );
    }

    @Test
    void exposesFabricCallbackCompatibleHandler() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("fabric|fb reload").executes(ctx -> Results.silent());
        FabricCommandRegistration<NativeSource> registration =
            FabricMinecraftAdapter.commandRegistration(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        registration.callback().register(dispatcher, new Object(), new Object());

        assertNotNull(dispatcher.getRoot().getChild("fabric"));
        assertNotNull(dispatcher.getRoot().getChild("fb"));
    }

    private record NativeSource() {
        CommandSource source() {
            return new CommandSource() {
            };
        }
    }
}
