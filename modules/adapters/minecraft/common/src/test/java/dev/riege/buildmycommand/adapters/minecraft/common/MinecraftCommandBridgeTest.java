package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterMatchingPolicy;
import dev.riege.buildmycommand.adapters.AdapterRegistrationLabels;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(List.of("ban", "block"), adapter.rootLabels());
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
        assertEquals(List.of("kick", "boot"), plan.rootLabels());
    }

    @Test
    void registrationPlanAcceptsGenericAdapterContract() {
        IAdapter<FakeSender, MinecraftInvocation, MinecraftRenderedResult> adapter =
            new ContractOnlyMinecraftAdapter(List.of("kick"), List.of("kick", "boot"));

        MinecraftCommandRegistrationPlan plan = MinecraftCommandRegistrationPlan.fromNativeAdapter(
            MinecraftBackendProfiles.spigot(),
            adapter
        );

        assertEquals(List.of("kick", "boot"), plan.rootLabels());
    }

    @Test
    void exposesSuggestionsForMinecraftCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("gamemode|gm set|apply <target:String> [--silent|-s]")
            .executes(ctx -> Results.silent());
        framework.registry()
            .route("wecc bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.success(ctx.arg("target", String.class)));

        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(framework, sender -> new CommandSource() {
        });

        assertEquals(List.of("gamemode"), bridge.suggest(new FakeSender(Set.of()), "/ga", 3));
        assertEquals(List.of("gamemode", "gm"), bridge.suggest(new FakeSender(Set.of()), "/g", 2));
        assertEquals(List.of("set", "apply"), bridge.suggest(new FakeSender(Set.of()), "/gamemode ", 10));
        assertEquals(List.of("bang"), bridge.suggest(new FakeSender(Set.of()), "/wecc b", 7));
        assertEquals(List.of("--silent", "-s"), bridge.suggest(new FakeSender(Set.of()),
            MinecraftInvocation.slash("/gamemode set Ada -", 19)));
    }

    @Test
    void canUseRootLabelHonorsAliasesCasePolicyPermissionsAndUnknownLabels() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();
        framework.registry()
            .route("ban|block <target:String>")
            .permission("mod.ban")
            .executes(ctx -> Results.success("ok"));
        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(
            framework,
            sender -> permissionSource(sender.permissions())
        );

        assertTrue(bridge.canUseRootLabel(new FakeSender(Set.of("mod.ban")), "ban"));
        assertTrue(bridge.canUseRootLabel(new FakeSender(Set.of("mod.ban")), "BLOCK"));
        assertFalse(bridge.canUseRootLabel(new FakeSender(Set.of()), "block"));
        assertTrue(bridge.canUseRootLabel(new FakeSender(Set.of()), "unknown"));
    }

    @Test
    void reconstructsPlatformInvocationsForSlashBrigadierAndArgsArrayBackends() {
        assertEquals("ban Alex griefing",
            MinecraftInvocation.slash("/ban Alex griefing", 7).normalizedInput());
        assertEquals("ban Alex",
            MinecraftInvocation.slash("ban Alex", 7).normalizedInput());
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
    void commandBridgeImplementsGenericAdapterSdkForMinecraftInvocations() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("msg", command -> command
            .alias("tell")
            .executes(ctx -> Results.success(ctx.commandInput().platform().id())));
        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(
            framework,
            sender -> permissionSource(sender.permissions())
        );

        CommandAdapter<FakeSender, MinecraftInvocation, CommandResult> adapter = bridge;
        CommandInput input = adapter.mapInput(new FakeSender(Set.of()), MinecraftInvocation.slash("/msg", 4));
        CommandResult rendered = adapter.execute(new FakeSender(Set.of()), MinecraftInvocation.slash("/msg", 4));

        assertEquals("minecraft", input.platform().id());
        assertEquals("/msg", input.rawInput());
        assertEquals("msg", input.normalizedInput());
        assertEquals(new AdapterCapabilities(false, true, true), adapter.capabilities());
        assertEquals(new AdapterRegistrationLabels(
            List.of("msg"),
            List.of("msg", "tell")
        ), adapter.registrationLabels());
        assertEquals(Optional.of("minecraft"), rendered.reply());
        assertEquals(CommandResult.Status.SUCCESS, adapter.renderer().render(rendered).status());
    }

    @Test
    void commandBridgeRejectsNullMappedSource() {
        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(
            CommandFramework.create(),
            sender -> null
        );

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> bridge.mapSource(new FakeSender(Set.of())));

        assertEquals("mapped source", exception.getMessage());
    }

    @Test
    void nativeAdapterExposesSdkConceptsWhileKeepingRegistrationLabelsList() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("home", command -> command
            .alias("h")
            .executes(ctx -> Results.success("ok")));
        MinecraftNativeCommandAdapter<FakeSender> adapter = new MinecraftNativeCommandAdapter<>(
            framework,
            sender -> permissionSource(sender.permissions())
        );

        assertEquals(List.of("home", "h"), adapter.rootLabels());
        assertEquals(new AdapterRegistrationLabels(
            List.of("home"),
            List.of("home", "h")
        ), adapter.registrationLabels());
        assertEquals(new AdapterCapabilities(false, true, true), adapter.capabilities());
        assertEquals("minecraft-native", adapter.config().adapterId());
        assertEquals("minecraft", adapter.runtime().platform().id());
        assertEquals("home", adapter.mapInput(new FakeSender(Set.of()), MinecraftInvocation.slash("/home", 5))
            .normalizedInput());
    }

    @Test
    void nativeAdapterRendersFailureAndSilentResultsThroughSdkContract() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("quiet", command -> command.executes(ctx -> Results.silent()));
        MinecraftNativeCommandAdapter<FakeSender> adapter = new MinecraftNativeCommandAdapter<>(
            framework,
            sender -> permissionSource(sender.permissions())
        );

        MinecraftRenderedResult failure = adapter.execute(new FakeSender(Set.of()), MinecraftInvocation.slash("/missing", 8));
        MinecraftRenderedResult silent = adapter.execute(new FakeSender(Set.of()), MinecraftInvocation.slash("/quiet", 6));

        assertEquals(0, failure.numericResult());
        assertEquals(Optional.of("Unknown command: missing"), failure.message());
        assertEquals(0, silent.numericResult());
        assertEquals(Optional.empty(), silent.message());
        assertEquals(Optional.empty(), MinecraftRenderedResult.of(0, null).message());
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
        assertEquals(List.of("warp"), plan.rootLabels());
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

    @Test
    void commonValueObjectsRejectInvalidInputsAndClampCursors() {
        assertThrows(NullPointerException.class, () -> new MinecraftInvocation(null, "ban", "ban", List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftInvocation("", " ", "ban", List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftInvocation("", "ban", " ", List.of(), 0));
        assertThrows(NullPointerException.class, () -> new MinecraftInvocation("", "ban", "ban", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftInvocation("", "ban", "ban", List.of(), -1));
        assertThrows(NullPointerException.class, () -> MinecraftInvocation.slash(null, 0));
        assertThrows(NullPointerException.class, () -> MinecraftInvocation.labelAndArgs("ban", null, 0));
        assertThrows(IllegalArgumentException.class, () -> MinecraftInvocation.labelAndArgs(" ", new String[] {}, 0));
        assertThrows(IllegalArgumentException.class, () -> MinecraftInvocation.labelAndArgs("ban", new String[] {}, -1));
        assertEquals(3, MinecraftInvocation.slash("/ban", 99).cursor());
        assertEquals(0, MinecraftInvocation.slash("/ban", 0).cursor());
        assertEquals(Optional.empty(), MinecraftInvocation.labelAndArgs("ban", new String[] {}, 0).currentArgPrefix());
        assertEquals(Optional.of(""), MinecraftInvocation.slash("/ban ", 5).currentArgPrefix());

        assertThrows(IllegalArgumentException.class, () -> new MinecraftRenderedResult(-1, "nope"));
        assertThrows(NullPointerException.class, () -> new MinecraftBackendProfile(null, "Backend", Set.of(), Set.of(), true));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBackendProfile(" ", "Backend", Set.of(), Set.of(), true));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBackendProfile("id", " ", Set.of(), Set.of(), true));
        assertThrows(NullPointerException.class, () -> new MinecraftBackendProfile("id", "Backend", null, Set.of(), true));
        assertThrows(NullPointerException.class, () -> new MinecraftBackendProfile("id", "Backend", Set.of(), null, true));
        assertThrows(NullPointerException.class, () -> new MinecraftRuntimeDescriptor(null, "1.20", "api", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftRuntimeDescriptor("paper", " ", "api", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftRuntimeDescriptor("paper", "1.20", " ", Set.of()));
        assertThrows(NullPointerException.class, () -> new MinecraftRuntimeDescriptor("paper", "1.20", "api", null));
        assertThrows(NullPointerException.class, () -> new MinecraftRuntimeDescriptor("paper", "1.20", "api", Set.of())
            .supports(null));
        assertThrows(NullPointerException.class, () -> new MinecraftSourceDescriptor(null, "Alex", new Object()));
        assertTrue(new MinecraftSourceDescriptor(MinecraftSourceKind.CONSOLE, null, null).name().isEmpty());
        assertTrue(new MinecraftSourceDescriptor(MinecraftSourceKind.CONSOLE, null, null).nativeHandleOptional().isEmpty());
        assertThrows(NullPointerException.class, () -> new MinecraftCommandRegistrationPlan(null, List.of("ban"), 0, true));
        assertThrows(NullPointerException.class, () -> new MinecraftCommandRegistrationPlan(MinecraftBackendProfiles.paper(), null, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftCommandRegistrationPlan(MinecraftBackendProfiles.paper(),
            List.of("ban"),
            -1,
            true));
    }

    @Test
    void bridgeAndNativeAdapterExposeNullContractsAndDelegates() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("root child")
            .permission("root.child")
            .executes(ctx -> Results.success("child"));
        MinecraftCommandBridge<FakeSender> bridge = new MinecraftCommandBridge<>(framework, sender -> permissionSource(sender.permissions()));
        MinecraftNativeCommandAdapter<FakeSender> nativeAdapter = new MinecraftNativeCommandAdapter<>(
            bridge,
            MinecraftResultRenderer.defaultRenderer()
        );

        assertThrows(NullPointerException.class, () -> new MinecraftCommandBridge<FakeSender>(null, sender -> new CommandSource() {
        }));
        assertThrows(NullPointerException.class, () -> new MinecraftCommandBridge<>(framework, null));
        assertThrows(NullPointerException.class, () -> new MinecraftNativeCommandAdapter<FakeSender>(null,
            MinecraftResultRenderer.defaultRenderer()));
        assertThrows(NullPointerException.class, () -> new MinecraftNativeCommandAdapter<>(bridge, null));
        assertThrows(NullPointerException.class, () -> bridge.dispatch(null, "root child"));
        assertThrows(NullPointerException.class, () -> bridge.dispatch(new FakeSender(Set.of()), (String) null));
        assertThrows(NullPointerException.class, () -> bridge.dispatch(new FakeSender(Set.of()), (MinecraftInvocation) null));
        assertThrows(NullPointerException.class, () -> bridge.suggest(null, "root", 0));
        assertThrows(NullPointerException.class, () -> bridge.suggest(new FakeSender(Set.of()), (String) null, 0));
        assertThrows(NullPointerException.class, () -> bridge.suggest(new FakeSender(Set.of()), (MinecraftInvocation) null));
        assertThrows(NullPointerException.class, () -> bridge.suggestRich(new FakeSender(Set.of()), null, 0));
        assertThrows(NullPointerException.class, () -> bridge.mapSource(null));
        assertThrows(NullPointerException.class, () -> bridge.mapInput(null, MinecraftInvocation.slash("/root", 5)));
        assertThrows(NullPointerException.class, () -> bridge.mapInput(new FakeSender(Set.of()), null));
        assertThrows(NullPointerException.class, () -> MinecraftAdapterContracts.rootLabels(null));
        assertThrows(NullPointerException.class, () -> nativeAdapter.canUseRootLabel(null, "root"));
        assertThrows(NullPointerException.class, () -> nativeAdapter.canUseRootLabel(new FakeSender(Set.of()), null));

        assertFalse(nativeAdapter.matchingPolicy().caseInsensitiveLiterals());
        assertFalse(bridge.canUseRootLabel(new FakeSender(Set.of()), "root"));
        assertTrue(nativeAdapter.canUseRootLabel(new FakeSender(Set.of("root.child")), "root"));
        assertTrue(bridge.canUseRootLabel(new FakeSender(Set.of("root.child")), "root"));
        assertEquals(Optional.of("child"), bridge.dispatch(new FakeSender(Set.of("root.child")), "root child").reply());
        assertEquals("root", nativeAdapter.mapInput(new FakeSender(Set.of()), MinecraftInvocation.slash("/root", 5))
            .normalizedInput());
        assertTrue(nativeAdapter.mapSource(new FakeSender(Set.of("root.child"))).hasPermission("root.child"));
        assertEquals(Optional.of("child"), nativeAdapter.dispatch(new FakeSender(Set.of("root.child")),
            MinecraftInvocation.slash("/root child", 11)).reply());
        assertEquals(List.of("root"), nativeAdapter.suggest(new FakeSender(Set.of("root.child")), MinecraftInvocation.slash("/ro", 3)));
        assertEquals(List.of("root"), nativeAdapter.suggestRich(new FakeSender(Set.of("root.child")),
            MinecraftInvocation.slash("/ro", 3),
            2).stream().map(Suggestion::value).toList());
    }

    @Test
    void sharedBrigadierFactoriesAndRegistrationHelpersCoverAllCommonBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms ping").executes(ctx -> Results.silent());

        assertEquals("minestom", MinecraftBackendProfiles.minestom().id());
        assertEquals("sponge", MinecraftBackendProfiles.sponge().id());
        assertTrue(MinecraftBackendProfiles.sponge().capabilities().contains(MinecraftCapability.EVENT_BUS));

        var defaultBridge = MinecraftBrigadierAdapters.create(framework, sender -> new CommandSource() {
        });
        var spongeBridge = MinecraftBrigadierAdapters.create(MinecraftBackendProfiles.sponge(), framework, sender -> new CommandSource() {
        });
        MinecraftCommandRegistrationPlan plan = MinecraftCommandRegistrationPlans.from(MinecraftBackendProfiles.sponge(), spongeBridge);

        assertEquals("minecraft-fabric-brigadier", defaultBridge.config().adapterId());
        assertEquals("minecraft-sponge-brigadier", spongeBridge.config().adapterId());
        assertEquals(List.of("minestom", "ms"), plan.rootLabels());
        assertTrue(plan.reloadSafe());
        assertThrows(NullPointerException.class, () -> MinecraftBrigadierAdapters.create(null, framework, sender -> new CommandSource() {
        }));
        assertThrows(NullPointerException.class, () -> MinecraftCommandRegistrationPlan.fromBridge(
            MinecraftBackendProfiles.paper(),
            null
        ));
        assertThrows(NullPointerException.class, () -> MinecraftCommandRegistrationPlan.fromNativeAdapter(
            MinecraftBackendProfiles.paper(),
            null
        ));
        assertThrows(NullPointerException.class, () -> MinecraftCommandRegistrationPlans.from(null, spongeBridge));
        assertThrows(NullPointerException.class, () -> MinecraftCommandRegistrationPlans.from(MinecraftBackendProfiles.sponge(), null));
    }

    @Test
    void utilityClassesKeepPrivateConstructorsForStaticOnlyContracts() throws Exception {
        assertUtilityConstructor(MinecraftAdapterContracts.class);
        assertUtilityConstructor(MinecraftBackendProfiles.class);
        assertUtilityConstructor(MinecraftBrigadierAdapters.class);
        assertUtilityConstructor(MinecraftCommandRegistrationPlans.class);
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

    private static void assertUtilityConstructor(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    private static final class ContractOnlyMinecraftAdapter
        implements IAdapter<FakeSender, MinecraftInvocation, MinecraftRenderedResult> {
        private final AdapterRegistrationLabels labels;
        private final AdapterRuntime runtime = new AdapterRuntime(CommandFramework.create(), CommandPlatform.test());
        private final AdapterConfig config = AdapterConfig.of(
            "contract-only-minecraft",
            "Contract Only Minecraft",
            new AdapterCapabilities(false, true, true)
        );

        private ContractOnlyMinecraftAdapter(List<String> roots, List<String> allLabels) {
            this.labels = new AdapterRegistrationLabels(roots, allLabels);
        }

        @Override
        public AdapterRuntime runtime() {
            return runtime;
        }

        @Override
        public AdapterConfig config() {
            return config;
        }

        @Override
        public AdapterCapabilities capabilities() {
            return config.capabilities();
        }

        @Override
        public AdapterMatchingPolicy matchingPolicy() {
            return AdapterMatchingPolicy.strict();
        }

        @Override
        public AdapterRegistrationLabels registrationLabels() {
            return labels;
        }

        @Override
        public AdapterRenderer<MinecraftRenderedResult> renderer() {
            return MinecraftResultRenderer.defaultRenderer()::render;
        }

        @Override
        public CommandSource mapSource(FakeSender nativeSource) {
            return permissionSource(nativeSource.permissions());
        }

        @Override
        public CommandInput mapInput(FakeSender nativeSource, MinecraftInvocation nativeInput) {
            return CommandInput.raw(mapSource(nativeSource), nativeInput.normalizedInput());
        }

        @Override
        public CommandResult dispatch(FakeSender nativeSource, MinecraftInvocation nativeInput) {
            return Results.silent();
        }

        @Override
        public List<Suggestion> suggestRich(FakeSender nativeSource, MinecraftInvocation nativeInput, int cursor) {
            return List.of();
        }

        @Override
        public List<String> suggest(FakeSender nativeSource, MinecraftInvocation nativeInput, int cursor) {
            return List.of();
        }

        @Override
        public MinecraftRenderedResult render(CommandResult result) {
            return renderer().render(result);
        }

        @Override
        public MinecraftRenderedResult execute(FakeSender nativeSource, MinecraftInvocation nativeInput) {
            return render(dispatch(nativeSource, nativeInput));
        }
    }
}
