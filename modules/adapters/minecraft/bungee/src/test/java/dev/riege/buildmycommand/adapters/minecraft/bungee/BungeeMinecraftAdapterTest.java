package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BungeeMinecraftAdapterTest {
    @Test
    void keepsBoundLabelForBungeeTabExecutor() {
        assertTrue(BungeeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.BUNGEE_TAB_COMPLETE));
        assertEquals("server lobby", BungeeMinecraftAdapter.commandInput("server", new String[] {"lobby"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForProxyCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server|srv <name:String>").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = BungeeMinecraftAdapter.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("server", "srv"), adapter.registrationLabels());
    }
}
