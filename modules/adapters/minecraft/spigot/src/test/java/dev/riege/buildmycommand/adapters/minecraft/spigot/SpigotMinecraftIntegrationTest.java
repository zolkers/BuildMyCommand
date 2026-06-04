/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpigotMinecraftIntegrationTest {
    @Test
    void reconstructsCommandExecutorInvocation() {
        assertTrue(SpigotMinecraftIntegration.profile().edgeCases().contains(MinecraftCommandEdgeCase.ARGS_ARRAY));
        assertEquals("home set base", SpigotMinecraftIntegration.commandExecutorInput("home", new String[] {"set", "base"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForCommandExecutorBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("home|h set").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = SpigotMinecraftIntegration.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("home", "h"), adapter.rootLabels());
    }

    @Test
    void mapsBukkitCommandSenderToCommandSource() {
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        CommandSource source = SpigotMinecraftIntegration.commandSource(sender.proxy());

        assertEquals(Optional.of("Ada"), source.name());
        assertEquals(java.util.Locale.ROOT, source.locale());
        assertTrue(source.hasPermission("minecraft.command.ban"));
        assertTrue(source.hasPermission(""));
        assertFalse(source.hasPermission("minecraft.command.kick"));
        assertEquals(Optional.of(sender.proxy()), source.unwrap(CommandSender.class));
        assertEquals(Optional.empty(), source.unwrap(String.class));
        source.reply("hello");
        source.reply(CommandMessage.success("world"));
        assertEquals(List.of("hello", "world"), sender.messages());
        assertThrows(NullPointerException.class, () -> source.unwrap(null));
        assertThrows(NullPointerException.class, () -> source.reply((String) null));
        assertThrows(NullPointerException.class, () -> source.reply((CommandMessage) null));
        assertThrows(NullPointerException.class, () -> source.hasPermission(null));
        assertThrows(NullPointerException.class, () -> new SpigotCommandSource(null));
    }

    @Test
    void nativeCommandDelegatesExecutionAndRepliesRenderedResult() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String>")
            .executes(ctx -> Results.success("Banned " + ctx.arg("target", String.class)));
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "ban",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        assertSame(command.adapter(), command.adapter());
        assertTrue(command.execute(sender.proxy(), "block", new String[] {"Alex"}));
        assertEquals(List.of("Banned Alex"), sender.messages());
    }

    @Test
    void nativeTabCompleterReconstructsInvocationAndDelegatesSuggestions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> appeal|reason")
            .executes(ctx -> Results.silent());
        framework.registry().route("ban|block bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "ban",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        assertEquals(List.of("appeal"), command.onTabComplete(
            sender.proxy(),
            command,
            "block",
            new String[] {"Alex", "a"}
        ));
        assertEquals(List.of("bang"), command.onTabComplete(
            sender.proxy(),
            command,
            "block",
            new String[] {"b"}
        ));
    }

    @Test
    void nativeCommandTabCompletePathDelegatesSuggestions() throws Exception {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String> appeal|reason")
            .executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "ban",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
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

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "block",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
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

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "block",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        assertEquals(List.of("appeal"), command.tabComplete(
            sender.proxy(),
            "buildmycommand:block",
            new String[] {"Alex", "a"}
        ));
        assertEquals(List.of(), command.tabComplete(
            sender.proxy(),
            "buildmycommand:",
            new String[] {"Alex", "a"}
        ));
    }

    @Test
    void nativeCommandReturnsTrueAfterRenderingFrameworkFailure() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban <target:String>")
            .executes(ctx -> Results.failure("Cannot ban " + ctx.arg("target", String.class)));
        RecordingSender sender = new RecordingSender("Ada", "minecraft.command.ban");

        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand(
            "ban",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        assertTrue(command.execute(sender.proxy(), "ban", new String[] {"Alex"}));
        assertEquals(List.of("Cannot ban Alex"), sender.messages());
    }

    @Test
    void registrationFacadeExposesAliasLabelsAndRegistersNativeCommands() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban|block <target:String>").executes(ctx -> Results.silent());
        RecordingCommandMap commandMap = new RecordingCommandMap();

        SpigotCommandRegistration registration = SpigotMinecraftIntegration.registration(
            "buildmycommand",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        assertEquals("buildmycommand", registration.fallbackPrefix());
        assertEquals(List.of(), registration.commands());
        assertSame(registration, registration.unregister(commandMap));
        assertEquals(List.of("ban", "block"), registration.labels());
        assertEquals(List.of("ban", "block"), registration.plan().rootLabels());
        assertEquals(List.of("ban", "block"), registration.register(commandMap));
        assertEquals(2, registration.commands().size());
        assertEquals(List.of(
            "ban",
            "buildmycommand:ban",
            "block",
            "buildmycommand:block"
        ), new ArrayList<>(commandMap.commands().keySet()));
        assertEquals(List.of("ban", "block"), registration.register(commandMap));
        assertSame(registration.adapter(), registration.unregister(commandMap).adapter());
        assertTrue(commandMap.commands().isEmpty());
    }

    @Test
    void registrationAndNativeCommandsRejectInvalidInputs() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban").executes(ctx -> Results.silent());
        var adapter = SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource);
        SpigotNativeCommand command = SpigotMinecraftIntegration.nativeCommand("ban", adapter);

        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.commandExecutorInput("ban", null));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.commandSource(null));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.nativeCommand(null, adapter));
        assertThrows(IllegalArgumentException.class, () -> SpigotMinecraftIntegration.nativeCommand(" ", adapter));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.nativeCommand("ban", null));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.registration(null, adapter));
        assertThrows(IllegalArgumentException.class, () -> SpigotMinecraftIntegration.registration(" ", adapter));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.registration("buildmycommand", null));
        assertThrows(NullPointerException.class, () -> command.onCommand(null, command, "ban", new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onCommand(new RecordingSender("Ada").proxy(), null, "ban", new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onCommand(new RecordingSender("Ada").proxy(), command, null, new String[] {}));
        assertThrows(IllegalArgumentException.class, () -> command.onCommand(new RecordingSender("Ada").proxy(), command, " ", new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onCommand(new RecordingSender("Ada").proxy(), command, "ban", null));
        assertThrows(NullPointerException.class, () -> command.onTabComplete(null, command, "ban", new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onTabComplete(new RecordingSender("Ada").proxy(), null, "ban", new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onTabComplete(new RecordingSender("Ada").proxy(), command, "ban", null));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.registration("buildmycommand", adapter).register(null));
        assertThrows(NullPointerException.class, () -> SpigotMinecraftIntegration.registration("buildmycommand", adapter).unregister(null));
    }

    @Test
    void unregisterFallsBackWhenCommandMapDoesNotExposeKnownCommands() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban").executes(ctx -> Results.silent());
        NoFieldCommandMap commandMap = new NoFieldCommandMap();
        SpigotCommandRegistration registration = SpigotMinecraftIntegration.registration(
            "buildmycommand",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        registration.register(commandMap);

        assertFalse(commandMap.registered().isEmpty());
        registration.unregister(commandMap);
        assertTrue(registration.commands().isEmpty());
        assertFalse(commandMap.registered().isEmpty());
    }

    @Test
    void unregisterHandlesAlternativeCommandMapFieldShapes() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("ban").executes(ctx -> Results.silent());
        StringFirstCommandMap commandMap = new StringFirstCommandMap();
        NullKnownCommandsMap nullMap = new NullKnownCommandsMap();
        SpigotCommandRegistration registration = SpigotMinecraftIntegration.registration(
            "buildmycommand",
            SpigotMinecraftIntegration.commandAdapter(framework, SpigotMinecraftIntegration::commandSource)
        );

        registration.register(commandMap);
        registration.unregister(commandMap);
        registration.register(nullMap);
        registration.unregister(nullMap);

        assertTrue(commandMap.commands.isEmpty());
        assertTrue(registration.commands().isEmpty());
    }

    @Test
    void reflectiveCommandMapFieldAccessFallsBackToNullWhenIllegal() throws Exception {
        Method fieldValue = SpigotCommandRegistration.class.getDeclaredMethod(
            "fieldValue",
            CommandMap.class,
            java.lang.reflect.Field.class
        );
        fieldValue.setAccessible(true);
        java.lang.reflect.Field secret = SecretCommandMap.class.getDeclaredField("secret");

        assertEquals(null, fieldValue.invoke(null, new SecretCommandMap(), secret));
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

    private static final class NoFieldCommandMap implements CommandMap {
        private final Map<String, Command> store = new LinkedHashMap<>();

        @Override
        public void registerAll(String fallbackPrefix, List<Command> commands) {
            commands.forEach(command -> register(fallbackPrefix, command));
        }

        @Override
        public boolean register(String label, String fallbackPrefix, Command command) {
            store.put(label, command);
            return true;
        }

        @Override
        public boolean register(String fallbackPrefix, Command command) {
            store.put(command.getName(), command);
            return true;
        }

        @Override
        public boolean dispatch(CommandSender sender, String commandLine) {
            return false;
        }

        @Override
        public void clearCommands() {
            store.clear();
        }

        @Override
        public Command getCommand(String name) {
            return store.get(name);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String cmdLine) {
            return List.of();
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String cmdLine, org.bukkit.Location location) {
            return List.of();
        }

        Map<String, Command> registered() {
            return store;
        }
    }

    private static final class StringFirstCommandMap implements CommandMap {
        @SuppressWarnings("unused")
        private final String knownCommands = "not a map";
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
    }

    private static final class NullKnownCommandsMap implements CommandMap {
        @SuppressWarnings("unused")
        private final Map<String, Command> knownCommands = null;

        @Override
        public void registerAll(String fallbackPrefix, List<Command> commands) {
        }

        @Override
        public boolean register(String label, String fallbackPrefix, Command command) {
            return true;
        }

        @Override
        public boolean register(String fallbackPrefix, Command command) {
            return true;
        }

        @Override
        public boolean dispatch(CommandSender sender, String commandLine) {
            return false;
        }

        @Override
        public void clearCommands() {
        }

        @Override
        public Command getCommand(String name) {
            return null;
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

    private static final class SecretCommandMap implements CommandMap {
        @SuppressWarnings("unused")
        private final String secret = "hidden";

        @Override
        public void registerAll(String fallbackPrefix, List<Command> commands) {
        }

        @Override
        public boolean register(String label, String fallbackPrefix, Command command) {
            return false;
        }

        @Override
        public boolean register(String fallbackPrefix, Command command) {
            return false;
        }

        @Override
        public boolean dispatch(CommandSender sender, String commandLine) {
            return false;
        }

        @Override
        public void clearCommands() {
        }

        @Override
        public Command getCommand(String name) {
            return null;
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
