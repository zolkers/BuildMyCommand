package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.spigot.SpigotCommandRegistration;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperMinecraftIntegrationTest {
    @Test
    void exposesPaperBrigadierLifecycleProfile() {
        assertTrue(PaperMinecraftIntegration.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertTrue(PaperMinecraftIntegration.profile().edgeCases().contains(MinecraftCommandEdgeCase.LIFECYCLE_REREGISTRATION));
        assertEquals("warp set", PaperMinecraftIntegration.brigadierInput("/warp set", 9).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterAlongsideBrigadierProjection() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = PaperMinecraftIntegration.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals("warp set", PaperMinecraftIntegration.commandInput("warp", new String[] {"set"}).normalizedInput());
        assertEquals(List.of("warp", "w"), adapter.rootLabels());
    }

    @Test
    void commandSourceDelegatesToSpigotCommandSource() {
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[] {CommandSender.class},
            (ignored, method, args) -> switch (method.getName()) {
                case "getName" -> "Ada";
                case "hasPermission" -> true;
                case "sendMessage", "setOp" -> null;
                case "isPermissionSet", "isOp" -> false;
                case "spigot" -> null;
                default -> defaultReturn(method.getReturnType());
            }
        );

        assertEquals(java.util.Optional.of("Ada"), PaperMinecraftIntegration.commandSource(sender).name());
    }

    @Test
    void selectsExplicitRegistrationStrategiesAndRejectsReservedHybrid() {
        assertEquals(PaperCommandRegistrationMode.BRIGADIER_PROJECTION,
            PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.BRIGADIER_PROJECTION).mode());
        assertEquals(PaperCommandRegistrationMode.NATIVE_COMMAND,
            PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.NATIVE_COMMAND).mode());
        assertTrue(PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.BRIGADIER_PROJECTION).usesBrigadierProjection());
        assertTrue(PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.NATIVE_COMMAND).usesNativeCommandMapFallback());
        assertEquals(false, PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.NATIVE_COMMAND).usesBrigadierProjection());
        assertEquals(false, PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.BRIGADIER_PROJECTION)
            .usesNativeCommandMapFallback());

        UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
            () -> PaperMinecraftIntegration.strategy(PaperCommandRegistrationMode.HYBRID));

        assertEquals("Paper HYBRID registration is reserved for a future native Brigadier + command-map bridge.",
            error.getMessage());
    }

    @Test
    void nativeRegistrationDelegatesToSpigotCommandMapFallback() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        MinecraftNativeCommandAdapter<CommandSender> adapter = PaperMinecraftIntegration.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        PaperNativeCommandRegistration registration = PaperMinecraftIntegration.nativeRegistration(
            "buildmycommand",
            adapter
        );

        assertEquals(PaperCommandRegistrationMode.NATIVE_COMMAND, registration.mode());
        assertEquals("buildmycommand", registration.fallbackPrefix());
        assertEquals(List.of("warp", "w"), registration.labels());
        assertEquals(List.of(), registration.commands());
        assertSame(adapter, registration.adapter());
        SpigotCommandRegistration spigotFallback = registration.spigotFallback();
        assertEquals("buildmycommand", spigotFallback.fallbackPrefix());
        assertSame(adapter, spigotFallback.adapter());

        RecordingCommandMap commandMap = new RecordingCommandMap();
        assertEquals(List.of("warp", "w"), registration.register(commandMap));
        assertEquals(2, registration.commands().size());
        assertSame(registration, registration.unregister(commandMap));
        assertEquals(List.of(), registration.commands());
    }

    @Test
    void brigadierLifecycleFacadeExposesLabelsPlanAndExactLiteralSemantics() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());

        PaperBrigadierRegistration registration = PaperMinecraftIntegration.brigadierRegistration(
            framework,
            sender -> new CommandSource() {
            }
        );
        MinecraftCommandRegistrationPlan plan = registration.plan();

        assertEquals(PaperCommandRegistrationMode.BRIGADIER_PROJECTION, registration.mode());
        assertEquals(List.of("warp", "w"), registration.labels());
        assertEquals(List.of("warp", "w"), registration.roots().stream()
            .map(com.mojang.brigadier.tree.LiteralCommandNode::getLiteral)
            .toList());
        assertEquals(List.of("warp", "w"), registration.rootLiterals());
        assertEquals(List.of("warp", "w"), plan.rootLiterals());
        assertEquals(List.of("warp", "w"), plan.rootLabels());
        assertTrue(plan.reloadSafe());
        assertTrue(registration.exactLiteralMatching());
        assertTrue(registration.frameworkAuthoritativeMatching());
        assertEquals("minecraft-paper-brigadier", registration.bridge().config().adapterId());
        assertEquals(
            "Paper Brigadier projection exposes exact native nodes and delegates framework matching through _bmc_input fallbacks.",
            registration.matchingNotice()
        );
    }

    @Test
    void brigadierLifecycleFacadeRegistersProjectedRootsWithPaperCommandsRegistrar() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        PaperBrigadierRegistration registration = PaperMinecraftIntegration.brigadierRegistration(
            framework,
            sender -> new CommandSource() {
            }
        );
        List<String> registered = new ArrayList<>();
        Commands commands = recordingCommands(registered);

        assertEquals(new LinkedHashSet<>(List.of("warp", "w")), registration.register(commands));
        assertEquals(List.of("warp aliases=[w]"), registered);
    }

    @Test
    void brigadierLifecycleFacadeAttachesToPluginLifecycleManager() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        PaperBrigadierRegistration registration = PaperMinecraftIntegration.brigadierRegistration(
            framework,
            sender -> new CommandSource() {
            }
        );
        List<String> lifecycleCalls = new ArrayList<>();

        assertSame(registration, registration.attachLifecycle(plugin(lifecycleCalls)));

        assertEquals(List.of("registerEventHandler"), lifecycleCalls);
    }

    @Test
    void adapterFacadeRejectsInvalidInputsAndExposesSpigotFallbacks() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp").executes(ctx -> Results.silent());
        MinecraftNativeCommandAdapter<CommandSender> adapter = PaperMinecraftIntegration.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals("minecraft-paper-brigadier", PaperMinecraftIntegration.brigadierBridge(framework, sender -> new CommandSource() {
        }).config().adapterId());
        assertEquals("warp", PaperMinecraftIntegration.nativeCommand("warp", adapter).getName());
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.brigadierInput(null, 0));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.commandInput("warp", null));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.strategy(null));
        assertThrows(NullPointerException.class, () -> new PaperBrigadierRegistration(null,
            PaperMinecraftIntegration.brigadierRegistration(framework, sender -> new CommandSource() {
            }).bridge()));
        assertThrows(NullPointerException.class, () -> new PaperBrigadierRegistration(PaperMinecraftIntegration.profile(), null));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.brigadierRegistration(framework,
            sender -> new CommandSource() {
            }).register(null));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.brigadierRegistration(framework,
            sender -> new CommandSource() {
            }).registrationHandler().run(null));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.brigadierRegistration(framework,
            sender -> new CommandSource() {
            }).attachLifecycle(null));
        assertThrows(NullPointerException.class, () -> new PaperNativeCommandRegistration(null));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.nativeRegistration(null, adapter));
        assertThrows(NullPointerException.class, () -> PaperMinecraftIntegration.nativeRegistration("buildmycommand", null));
    }

    @Test
    void brigadierLifecycleFacadeAttachesToPaperCommandsLifecycleEvent() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        PaperBrigadierRegistration registration = PaperMinecraftIntegration.brigadierRegistration(
            framework,
            sender -> new CommandSource() {
            }
        );
        List<String> registered = new ArrayList<>();

        registration.registrationHandler().run(commandsEvent(recordingCommands(registered)));

        assertEquals(List.of("warp aliases=[w]"), registered);
    }

    private static Commands recordingCommands(List<String> registered) {
        return (Commands) Proxy.newProxyInstance(
            Commands.class.getClassLoader(),
            new Class<?>[] {Commands.class},
            (ignored, method, args) -> {
                if ("register".equals(method.getName())
                    && args != null
                    && args.length > 0
                    && args[0] instanceof com.mojang.brigadier.tree.LiteralCommandNode<?> node
                ) {
                    if (args.length == 2 && args[1] instanceof Collection<?> aliases) {
                        registered.add(node.getLiteral() + " aliases=" + aliases);
                        LinkedHashSet<String> labels = new LinkedHashSet<>();
                        labels.add(node.getLiteral());
                        aliases.forEach(alias -> labels.add((String) alias));
                        return labels;
                    }
                    registered.add(node.getLiteral() + " aliases=[]");
                    return Set.of(node.getLiteral());
                }
                if ("toString".equals(method.getName())) {
                    return "RecordingCommands";
                }
                return null;
            }
        );
    }

    private static ReloadableRegistrarEvent<Commands> commandsEvent(Commands commands) {
        return (ReloadableRegistrarEvent<Commands>) Proxy.newProxyInstance(
            ReloadableRegistrarEvent.class.getClassLoader(),
            new Class<?>[] {ReloadableRegistrarEvent.class},
            (ignored, method, args) -> switch (method.getName()) {
                case "registrar" -> commands;
                case "toString" -> "CommandsEvent";
                default -> null;
            }
        );
    }

    private static Plugin plugin(List<String> lifecycleCalls) {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (ignored, method, args) -> {
                if ("getLifecycleManager".equals(method.getName())) {
                    return Proxy.newProxyInstance(
                        method.getReturnType().getClassLoader(),
                        new Class<?>[] {method.getReturnType()},
                        (ignoredLifecycle, lifecycleMethod, lifecycleArgs) -> {
                            if ("registerEventHandler".equals(lifecycleMethod.getName())) {
                                lifecycleCalls.add(lifecycleMethod.getName());
                            }
                            return null;
                        }
                    );
                }
                if ("toString".equals(method.getName())) {
                    return "Plugin";
                }
                return null;
            }
        );
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == float.class || type == double.class) {
            return 0.0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class RecordingCommandMap implements CommandMap {
        private final Map<String, Command> commands = new LinkedHashMap<>();

        @Override
        public void registerAll(String fallbackPrefix, List<Command> commands) {
            commands.forEach(command -> register(fallbackPrefix, command));
        }

        @Override
        public boolean register(String label, String fallbackPrefix, Command command) {
            commands.put(label, command);
            commands.put(fallbackPrefix + ":" + label, command);
            return true;
        }

        @Override
        public boolean register(String fallbackPrefix, Command command) {
            commands.put(command.getName(), command);
            return true;
        }

        @Override
        public boolean dispatch(CommandSender sender, String commandLine) {
            return false;
        }

        @Override
        public void clearCommands() {
            commands.clear();
        }

        @Override
        public Command getCommand(String name) {
            return commands.get(name);
        }

        @Override
        public Map<String, Command> getKnownCommands() {
            return commands;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String cmdLine) {
            return List.of();
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String cmdLine, org.bukkit.Location location) {
            return List.of();
        }
    }
}
