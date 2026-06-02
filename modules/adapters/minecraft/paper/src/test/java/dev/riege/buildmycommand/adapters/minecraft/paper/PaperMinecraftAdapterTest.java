package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperMinecraftAdapterTest {
    @Test
    void exposesPaperBrigadierLifecycleProfile() {
        assertTrue(PaperMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertTrue(PaperMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.LIFECYCLE_REREGISTRATION));
        assertEquals("warp set", PaperMinecraftAdapter.brigadierInput("/warp set", 9).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterAlongsideBrigadierProjection() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = PaperMinecraftAdapter.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals("warp set", PaperMinecraftAdapter.commandInput("warp", new String[] {"set"}).normalizedInput());
        assertEquals(List.of("warp", "w"), adapter.registrationLabels());
    }
}
