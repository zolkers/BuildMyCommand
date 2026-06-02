package dev.riege.buildmycommand.adapters.minecraft.forge;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeMinecraftAdapterTest {
    @Test
    void exposesRegisterCommandsEventProfile() {
        assertTrue(ForgeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
    }
}
