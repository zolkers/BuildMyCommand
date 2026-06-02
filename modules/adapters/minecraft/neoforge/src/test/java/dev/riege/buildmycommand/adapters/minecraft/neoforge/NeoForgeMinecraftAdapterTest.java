package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeMinecraftAdapterTest {
    @Test
    void exposesNeoForgeRegisterCommandsEventProfile() {
        assertTrue(NeoForgeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
    }
}
