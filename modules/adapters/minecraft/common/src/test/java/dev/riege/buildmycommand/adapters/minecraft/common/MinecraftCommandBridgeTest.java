package dev.riege.buildmycommand.adapters.minecraft.common;

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
        assertEquals(List.of("heal"), bridge.rootLabels());
    }

    @Test
    void nativeAdapterRegistersRootAliasesAndHonorsFrameworkCasePolicy() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        framework.registry()
            .route("ban|block <target:String> [--duration:Integer|-d] [--silent|-s]")
            .executes(ctx -> Results.success(ctx.arg("target", String.class)
                + ":"
                + ctx.option("duration", Integer.class).orElse(0)
                + ":"
                + ctx.flag("silent")));

        MinecraftNativeCommandAdapter<FakeSender> adapter = new MinecraftNativeCommandAdapter<>(
            framework,
            sender -> new CommandSource() {
            }
        );

        MinecraftRenderedResult result = adapter.execute(
            new FakeSender(Set.of()),
            MinecraftInvocation.labelAndArgs("BLOCK", new String[] {"Ada", "--Duration", "5", "-S"}, 3)
        );

        assertEquals(MinecraftAdapterMode.NATIVE_COMMAND, adapter.mode());
        assertEquals(List.of("ban", "block"), adapter.registrationLabels());
        assertTrue(adapter.honorsCaseInsensitiveLiterals());
        assertTrue(adapter.honorsCaseInsensitiveOptions());
        assertEquals(1, result.numericResult());
        assertEquals(Optional.of("Ada:5:true"), result.message());
    }

    @Test
    void registrationPlanForNativeAdapterUsesAllPlatformLabels() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("kick|boot <target:String>").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<FakeSender> adapter = new MinecraftNativeCommandAdapter<>(
            framework,
            sender -> new CommandSource() {
            }
        );

        MinecraftCommandRegistrationPlan plan = MinecraftCommandRegistrationPlan.fromNativeAdapter(
            MinecraftBackendProfiles.spigot(),
            adapter
        );

        assertEquals(List.of("kick", "boot"), plan.rootLiterals());
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
    void reconstructsPlatformInvocationsForSlashBrigadierAndArgsArrayBackends() {
        assertEquals("ban Alex griefing",
            MinecraftInvocation.slash("/ban Alex griefing", 7).normalizedInput());
        assertEquals("ban Alex griefing",
            MinecraftInvocation.labelAndArgs("ban", new String[] {"Alex", "griefing"}, 1).normalizedInput());
        assertEquals("ban ",
            MinecraftInvocation.labelAndArgs("ban", new String[] {""}, 1).normalizedInput());
        assertEquals(6,
            MinecraftInvocation.labelAndArgs("ban", new String[] {"Al"}, 1).cursor());
    }

    @Test
    void adaptsDispatchAndSuggestionFromPlatformInvocationObjects() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("ban <target:String> [reason:String...]")
            .permission("mod.ban")
            .executes(ctx -> Results.success(ctx.arg("target", String.class)
                + ":"
                + ctx.optionalArg("reason", String.class).orElse("none")));

        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(
            framework,
            sender -> permissionSource(sender.permissions())
        );

        CommandResult result = bridge.dispatch(new FakeSender(Set.of("mod.ban")),
            MinecraftInvocation.labelAndArgs("ban", new String[] {"Alex", "griefing"}, 1));
        List<String> deniedSuggestions = bridge.suggest(new FakeSender(Set.of()), MinecraftInvocation.slash("/ba", 3));

        assertEquals(Optional.of("Alex:griefing"), result.reply());
        assertEquals(List.of(), deniedSuggestions);
    }

    @Test
    void profilesCoverMajorMinecraftCommandBackendsAndTheirEdgeCases() {
        assertEquals(Set.of(
                MinecraftCommandEdgeCase.SLASH_PREFIX,
                MinecraftCommandEdgeCase.BRIGADIER_CURSOR,
                MinecraftCommandEdgeCase.LIFECYCLE_REREGISTRATION,
                MinecraftCommandEdgeCase.PERMISSION_FILTERING
            ),
            MinecraftBackendProfiles.paper().edgeCases());
        assertTrue(MinecraftBackendProfiles.spigot().edgeCases().contains(MinecraftCommandEdgeCase.ARGS_ARRAY));
        assertTrue(MinecraftBackendProfiles.bungee().edgeCases().contains(MinecraftCommandEdgeCase.BUNGEE_TAB_COMPLETE));
        assertTrue(MinecraftBackendProfiles.velocity().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertTrue(MinecraftBackendProfiles.fabric().edgeCases().contains(MinecraftCommandEdgeCase.DEDICATED_ENVIRONMENT));
        assertTrue(MinecraftBackendProfiles.forge().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
        assertTrue(MinecraftBackendProfiles.neoforge().edgeCases().contains(MinecraftCommandEdgeCase.EVENT_BUS_REGISTRATION));
    }

    @Test
    void registrationPlanKeepsRootLiteralsStableForReloadableBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp set <name:String>").executes(ctx -> Results.silent());
        framework.registry().route("warp delete <name:String>").executes(ctx -> Results.silent());

        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(framework, sender -> new CommandSource() {
        });

        MinecraftCommandRegistrationPlan plan = MinecraftCommandRegistrationPlan.fromBridge(
            MinecraftBackendProfiles.paper(),
            bridge
        );

        assertEquals(MinecraftBackendProfiles.paper(), plan.backend());
        assertEquals(List.of("warp"), plan.rootLiterals());
        assertTrue(plan.reloadSafe());
    }

    @Test
    void sourceDescriptorAndResultRenderingCapturePlatformSpecificEdges() {
        MinecraftSourceDescriptor source = new MinecraftSourceDescriptor(
            MinecraftSourceKind.PLAYER,
            "Alex",
            "native-player-handle"
        );
        MinecraftRenderedResult success = MinecraftResultRenderer.defaultRenderer()
            .render(Results.success("ok"));
        MinecraftRenderedResult silent = MinecraftResultRenderer.defaultRenderer()
            .render(Results.silent());

        assertEquals(MinecraftSourceKind.PLAYER, source.kind());
        assertEquals(Optional.of("Alex"), source.name());
        assertEquals(1, success.numericResult());
        assertEquals(Optional.of("ok"), success.message());
        assertEquals(0, silent.numericResult());
        assertEquals(Optional.empty(), silent.message());
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

    private static CommandSource permissionSource(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }
}
