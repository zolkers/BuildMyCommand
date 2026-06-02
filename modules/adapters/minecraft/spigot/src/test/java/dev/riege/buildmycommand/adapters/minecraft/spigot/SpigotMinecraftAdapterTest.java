package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpigotMinecraftAdapterTest {
    @Test
    void reconstructsCommandExecutorInvocation() {
        assertTrue(SpigotMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.ARGS_ARRAY));
        assertEquals("home set base", SpigotMinecraftAdapter.commandExecutorInput("home", new String[] {"set", "base"}).normalizedInput());
    }
}
