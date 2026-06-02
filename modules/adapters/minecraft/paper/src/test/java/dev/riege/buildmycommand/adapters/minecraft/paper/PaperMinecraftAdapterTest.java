package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperMinecraftAdapterTest {
    @Test
    void exposesPaperBrigadierLifecycleProfile() {
        assertTrue(PaperMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertTrue(PaperMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.LIFECYCLE_REREGISTRATION));
        assertEquals("warp set", PaperMinecraftAdapter.brigadierInput("/warp set", 9).normalizedInput());
    }
}
