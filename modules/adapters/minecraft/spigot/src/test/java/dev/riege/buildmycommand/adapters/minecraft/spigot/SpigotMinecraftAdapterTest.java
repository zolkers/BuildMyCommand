package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpigotMinecraftAdapterTest {
    @Test
    void reconstructsCommandExecutorInvocation() {
        assertTrue(SpigotMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.ARGS_ARRAY));
        assertEquals("home set base", SpigotMinecraftAdapter.commandExecutorInput("home", new String[] {"set", "base"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForCommandExecutorBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("home|h set").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = SpigotMinecraftAdapter.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("home", "h"), adapter.registrationLabels());
    }
}
