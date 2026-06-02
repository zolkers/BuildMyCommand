package dev.riege.buildmycommand.minecraft;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftCommandBridgeTest {
    @Test
    void dispatchesSlashCommandsThroughFrameworkWithMappedSource() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("heal <target:String>")
            .permission("mod.heal")
            .executes(ctx -> Results.success(ctx.arg("target", String.class)));

        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(
            framework,
            sender -> new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return sender.permissions().contains(permission);
                }
            }
        );

        CommandResult result = bridge.dispatch(new FakeSender(Set.of("mod.heal")), "/heal Alex");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Alex"), result.reply());
        assertEquals(List.of("heal"), bridge.rootLiterals());
    }

    @Test
    void exposesSuggestionsForMinecraftCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("gamemode set <target:String>").executes(ctx -> Results.silent());

        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(framework, sender -> new CommandSource() {
        });

        assertEquals(List.of("gamemode"), bridge.suggest(new FakeSender(Set.of()), "/ga", 3));
    }

    @Test
    void describesRuntimeCapabilitiesWithoutBindingToOneLoaderVersion() {
        MinecraftRuntimeDescriptor runtime = new MinecraftRuntimeDescriptor(
            "paper",
            "1.21.8",
            "1.21.8-R0.1-SNAPSHOT",
            Set.of(MinecraftCapability.BRIGADIER, MinecraftCapability.LIFECYCLE_EVENTS)
        );

        assertTrue(runtime.supports(MinecraftCapability.BRIGADIER));
        assertTrue(runtime.supports(MinecraftCapability.LIFECYCLE_EVENTS));
        assertFalse(runtime.supports(MinecraftCapability.LEGACY_COMMAND_MAP));
        assertEquals("paper 1.21.8 (api 1.21.8-R0.1-SNAPSHOT)", runtime.displayName());
    }

    private record FakeSender(Set<String> permissions) {
    }
}
