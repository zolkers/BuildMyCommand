package dev.riege.buildmycommand.adapters.minecraft.fabric;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricMinecraftAdapterTest {
    @Test
    void exposesCommandRegistrationCallbackConstraints() {
        assertTrue(FabricMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.DEDICATED_ENVIRONMENT));
        assertEquals("fabric test", FabricMinecraftAdapter.brigadierInput("/fabric test", 12).normalizedInput());
    }
}
