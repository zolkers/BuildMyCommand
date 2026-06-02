package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityMinecraftAdapterTest {
    @Test
    void supportsProxyBrigadierCommands() {
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.PROXY_COMMANDS));
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertEquals("proxy reload", VelocityMinecraftAdapter.simpleCommandInput("proxy", new String[] {"reload"}).normalizedInput());
    }
}
