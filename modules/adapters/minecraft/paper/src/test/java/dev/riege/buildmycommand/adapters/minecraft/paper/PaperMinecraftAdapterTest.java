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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(List.of("warp", "w"), adapter.rootLabels());
    }

    @Test
    void selectsExplicitRegistrationStrategiesAndRejectsReservedHybrid() {
        assertEquals(PaperCommandRegistrationMode.BRIGADIER_PROJECTION,
            PaperMinecraftAdapter.strategy(PaperCommandRegistrationMode.BRIGADIER_PROJECTION).mode());
        assertEquals(PaperCommandRegistrationMode.NATIVE_COMMAND,
            PaperMinecraftAdapter.strategy(PaperCommandRegistrationMode.NATIVE_COMMAND).mode());

        UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
            () -> PaperMinecraftAdapter.strategy(PaperCommandRegistrationMode.HYBRID));

        assertEquals("Paper HYBRID registration is reserved for a future native Brigadier + command-map bridge.",
            error.getMessage());
    }

    @Test
    void nativeRegistrationDelegatesToSpigotCommandMapFallback() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        MinecraftNativeCommandAdapter<CommandSender> adapter = PaperMinecraftAdapter.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        PaperNativeCommandRegistration registration = PaperMinecraftAdapter.nativeRegistration(
            "buildmycommand",
            adapter
        );

        assertEquals(PaperCommandRegistrationMode.NATIVE_COMMAND, registration.mode());
        assertEquals(List.of("warp", "w"), registration.labels());
        assertSame(adapter, registration.adapter());
        SpigotCommandRegistration spigotFallback = registration.spigotFallback();
        assertEquals("buildmycommand", spigotFallback.fallbackPrefix());
        assertSame(adapter, spigotFallback.adapter());
    }

    @Test
    void brigadierLifecycleFacadeExposesLabelsPlanAndExactLiteralSemantics() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());

        PaperBrigadierRegistration registration = PaperMinecraftAdapter.brigadierRegistration(
            framework,
            sender -> new CommandSource() {
            }
        );
        MinecraftCommandRegistrationPlan plan = registration.plan();

        assertEquals(PaperCommandRegistrationMode.BRIGADIER_PROJECTION, registration.mode());
        assertEquals(List.of("warp", "w"), registration.labels());
        assertEquals(List.of("warp"), registration.rootLiterals());
        assertEquals(List.of("warp"), plan.rootLiterals());
        assertTrue(plan.reloadSafe());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "Paper Brigadier projection registers exact literal nodes; it does not add case-insensitive aliases.",
            registration.matchingNotice()
        );
    }

    @Test
    void brigadierLifecycleFacadeRegistersProjectedRootsWithPaperCommandsRegistrar() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        PaperBrigadierRegistration registration = PaperMinecraftAdapter.brigadierRegistration(
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
    void brigadierLifecycleFacadeAttachesToPaperCommandsLifecycleEvent() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("warp|w set <name:String>").executes(ctx -> Results.silent());
        PaperBrigadierRegistration registration = PaperMinecraftAdapter.brigadierRegistration(
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
                    if (args.length == 2 && args[1] instanceof java.util.Collection<?> aliases) {
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
}
