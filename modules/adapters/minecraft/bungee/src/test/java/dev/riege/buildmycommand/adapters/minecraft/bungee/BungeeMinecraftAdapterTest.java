package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BungeeMinecraftAdapterTest {
    @Test
    void keepsBoundLabelForBungeeTabExecutor() {
        assertTrue(BungeeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.BUNGEE_TAB_COMPLETE));
        assertEquals("server lobby", BungeeMinecraftAdapter.commandInput("server", new String[] {"lobby"}).normalizedInput());
    }
}
