package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void mapsBukkitCommandSenderToCommandSource() {
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        CommandSource source = SpigotMinecraftAdapter.commandSource(sender.proxy());

        assertEquals(Optional.of("Ada"), source.name());
        assertTrue(source.hasPermission("minecraft.command.ban"));
        assertEquals(Optional.of(sender.proxy()), source.unwrap(CommandSender.class));
        source.reply("hello");
        assertEquals(List.of("hello"), sender.messages());
    }

    @Test
    void nativeCommandDelegatesExecutionAndRepliesRenderedResult() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String>")
            .executes(ctx -> Results.success("Banned " + ctx.arg("target", String.class)));
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "ban",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertTrue(command.execute(sender.proxy(), "block", new String[] {"Alex"}));
        assertEquals(List.of("Banned Alex"), sender.messages());
    }

    @Test
    void nativeTabCompleterReconstructsInvocationAndDelegatesSuggestions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> appeal|reason")
            .executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "ban",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertEquals(List.of("appeal"), command.onTabComplete(
            sender.proxy(),
            command,
            "block",
            new String[] {"Alex", "a"}
        ));
    }

    @Test
    void nativeCommandTabCompletePathDelegatesSuggestions() throws Exception {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> appeal|reason")
            .executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "ban",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertEquals(List.of("appeal"), command.tabComplete(
            sender.proxy(),
            "block",
            new String[] {"Alex", "a"}
        ));
    }

    @Test
    void fallbackPrefixedCommandLabelsDispatchAsUnprefixedLabels() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String>")
            .executes(ctx -> Results.success("Blocked " + ctx.arg("target", String.class)));
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "block",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertTrue(command.execute(sender.proxy(), "buildmycommand:block", new String[] {"Alex"}));
        assertEquals(List.of("Blocked Alex"), sender.messages());
    }

    @Test
    void fallbackPrefixedTabCompleteLabelsSuggestAsUnprefixedLabels() throws Exception {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> appeal|reason")
            .executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "block",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertEquals(List.of("appeal"), command.tabComplete(
            sender.proxy(),
            "buildmycommand:block",
            new String[] {"Alex", "a"}
        ));
    }

    @Test
    void nativeCommandReturnsTrueAfterRenderingFrameworkFailure() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban <target:String>")
            .executes(ctx -> Results.failure("Cannot ban " + ctx.arg("target", String.class)));
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftAdapter.nativeCommand(
            "ban",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertTrue(command.execute(sender.proxy(), "ban", new String[] {"Alex"}));
        assertEquals(List.of("Cannot ban Alex"), sender.messages());
    }

    @Test
    void registrationFacadeExposesAliasLabelsAndRegistersNativeCommands() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String>").executes(ctx -> Results.silent());
        RecordingCommandMap commandMap = new RecordingCommandMap();

        SpigotCommandRegistration registration = SpigotMinecraftAdapter.registration(
            "buildmycommand",
            SpigotMinecraftAdapter.commandAdapter(framework, SpigotMinecraftAdapter::commandSource)
        );

        assertEquals(List.of("ban", "block"), registration.labels());
        assertEquals(List.of("ban", "block"), registration.register(commandMap));
        assertEquals(List.of(
            "ban",
            "buildmycommand:ban",
            "block",
            "buildmycommand:block"
        ), new ArrayList<>(commandMap.commands().keySet()));
        assertSame(registration.adapter(), registration.unregister(commandMap).adapter());
        assertTrue(commandMap.commands().isEmpty());
    }

    private static final class RecordingSender {
        private final String name;
        private final List<String> permissions;
        private final List<String> messages = new ArrayList<>();
        private final CommandSender proxy;

        RecordingSender(String name, String... permissions) {
            this.name = name;
            this.permissions = List.of(permissions);
            this.proxy = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "hasPermission" -> this.permissions.contains((String) args[0]);
                    case "sendMessage" -> {
                        if (args[0] instanceof String message) {
                            messages.add(message);
                        } else if (args[0] instanceof String[] batch) {
                            Collections.addAll(messages, batch);
                        }
                        yield null;
                    }
                    case "isPermissionSet" -> this.permissions.contains((String) args[0]);
                    case "isOp" -> false;
                    case "setOp" -> null;
                    case "spigot" -> null;
                    case "toString" -> "RecordingSender[" + name + "]";
                    default -> defaultReturn(method.getReturnType());
                }
            );
        }

        CommandSender proxy() {
            return proxy;
        }

        List<String> messages() {
            return messages;
        }
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
        public List<String> tabComplete(CommandSender sender, String cmdLine) {
            return List.of();
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String cmdLine, org.bukkit.Location location) {
            return List.of();
        }

        Map<String, Command> commands() {
            return commands;
        }
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
}
