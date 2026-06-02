package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityMinecraftAdapterTest {
    @Test
    void supportsProxyBrigadierCommands() {
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.PROXY_COMMANDS));
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertEquals("proxy reload", VelocityMinecraftAdapter.simpleCommandInput("proxy", new String[] {"reload"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForSimpleCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = VelocityMinecraftAdapter.simpleCommandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("proxy", "px"), adapter.registrationLabels());
    }
}
