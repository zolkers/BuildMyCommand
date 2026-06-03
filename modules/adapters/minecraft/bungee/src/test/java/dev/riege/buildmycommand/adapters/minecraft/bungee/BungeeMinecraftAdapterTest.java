package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandEdgeCase;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BungeeMinecraftAdapterTest {
    @Test
    void keepsBoundLabelForBungeeTabExecutor() {
        assertTrue(BungeeMinecraftAdapter.profile().edgeCases().contains(MinecraftCommandEdgeCase.BUNGEE_TAB_COMPLETE));
        assertEquals("server lobby", BungeeMinecraftAdapter.commandInput("server", new String[] {"lobby"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForProxyCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server|srv <name:String>").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = BungeeMinecraftAdapter.commandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("server", "srv"), adapter.rootLabels());
    }

    @Test
    void mapsBungeeSenderToCommandSource() {
        RecordingSender sender = new RecordingSender("Ada", "proxy.server");

        CommandSource source = BungeeMinecraftAdapter.commandSource(sender.proxy());

        assertEquals(Optional.of("Ada"), source.name());
        assertTrue(source.hasPermission("proxy.server"));
        assertEquals(Optional.empty(), source.unwrap(String.class));
        assertEquals(Optional.of(sender.proxy()), source.unwrap(CommandSender.class));
        source.reply("Hello");
        assertEquals(List.of("Hello"), sender.messages());
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.commandSource(null));
    }

    @Test
    void nativeCommandExecutesAndRendersResult() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server|srv <name:String>")
            .executes(ctx -> Results.success("Switching " + ctx.arg("name", String.class)));
        RecordingSender sender = new RecordingSender("Ada");

        BungeeNativeCommand command = BungeeMinecraftAdapter.nativeCommand(
            BungeeMinecraftAdapter.commandAdapter(framework)
        );

        command.execute(sender.proxy(), new String[] {"lobby"});

        assertEquals("server", command.getName());
        assertArrayEquals(new String[] {"srv"}, command.getAliases());
        assertSame(command.adapter(), command.adapter());
        assertEquals(List.of("Switching lobby"), sender.messages());
    }

    @Test
    void nativeTabExecutorDelegatesSuggestions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server|srv lobby").executes(ctx -> Results.silent());
        framework.registry().route("server|srv survival").executes(ctx -> Results.silent());
        RecordingSender sender = new RecordingSender("Ada");

        BungeeNativeCommand command = BungeeMinecraftAdapter.nativeCommand(
            BungeeMinecraftAdapter.commandAdapter(framework)
        );

        assertEquals(List.of("survival"), toList(command.onTabComplete(sender.proxy(), new String[] {"s"})));
    }

    @Test
    void registrationFacadeUsesRegistrarBoundaryAndUnregistersCommand() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server|srv <name:String>").executes(ctx -> Results.silent());
        Plugin plugin = uninitializedPlugin();
        RecordingRegistrar registrar = new RecordingRegistrar();

        BungeeCommandRegistration registration = BungeeMinecraftAdapter.registration(
            plugin,
            BungeeMinecraftAdapter.commandAdapter(framework)
        );

        assertThrows(RuntimeException.class, () -> registration.register(uninitializedPluginManager()));
        assertThrows(RuntimeException.class, () -> registration.unregister(uninitializedPluginManager()));
        assertSame(plugin, registration.plugin());
        assertSame(registration.adapter(), registration.adapter());
        assertSame(registration, registration.unregister(registrar));
        BungeeNativeCommand command = registration.register(registrar);

        assertSame(command, registration.command());
        assertEquals(List.of("server", "srv"), registration.labels());
        assertEquals(List.of("server", "srv"), registration.plan().rootLabels());
        assertSame(plugin, registrar.plugin());
        assertSame(command, registrar.registered());
        registration.unregister(registrar);
        assertSame(command, registrar.unregistered());
    }

    @Test
    void pluginManagerRegistrarDelegatesToBungeePluginManager() {
        Plugin plugin = uninitializedPlugin();
        PluginManager manager = uninitializedPluginManager();
        Command command = new BungeeNativeCommand("server", new String[] {}, BungeeMinecraftAdapter.commandAdapter(CommandFramework.create()));
        BungeeCommandRegistrar registrar = BungeeCommandRegistrar.pluginManager(manager);

        assertThrows(RuntimeException.class, () -> registrar.register(plugin, command));
        assertThrows(RuntimeException.class, () -> registrar.unregister(command));

        assertThrows(NullPointerException.class, () -> BungeeCommandRegistrar.pluginManager(null));
    }

    @Test
    void rejectsInvalidBungeeRegistrationAndNativeCommandInputs() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("server").executes(ctx -> Results.silent());
        var adapter = BungeeMinecraftAdapter.commandAdapter(framework);
        BungeeNativeCommand command = BungeeMinecraftAdapter.nativeCommand(adapter);
        Plugin plugin = uninitializedPlugin();

        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.commandInput("server", null));
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.nativeCommand(null));
        assertThrows(IllegalStateException.class, () -> BungeeMinecraftAdapter.nativeCommand(BungeeMinecraftAdapter.commandAdapter(CommandFramework.create())));
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.registration(null, adapter));
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.registration(plugin, null));
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.registration(plugin, adapter).register((BungeeCommandRegistrar) null));
        assertThrows(NullPointerException.class, () -> BungeeMinecraftAdapter.registration(plugin, adapter).unregister((BungeeCommandRegistrar) null));
        assertThrows(NullPointerException.class, () -> new BungeeNativeCommand(null, new String[] {}, adapter));
        assertThrows(IllegalArgumentException.class, () -> new BungeeNativeCommand(" ", new String[] {}, adapter));
        assertThrows(NullPointerException.class, () -> new BungeeNativeCommand("server", null, adapter));
        assertThrows(NullPointerException.class, () -> new BungeeNativeCommand("server", new String[] {}, null));
        assertThrows(NullPointerException.class, () -> command.execute(null, new String[] {}));
        assertThrows(NullPointerException.class, () -> command.execute(new RecordingSender("Ada").proxy(), null));
        assertThrows(NullPointerException.class, () -> command.onTabComplete(null, new String[] {}));
        assertThrows(NullPointerException.class, () -> command.onTabComplete(new RecordingSender("Ada").proxy(), null));
    }

    private static Plugin uninitializedPlugin() {
        return allocate(Plugin.class, "Unable to allocate Bungee Plugin test double");
    }

    private static PluginManager uninitializedPluginManager() {
        return allocate(PluginManager.class, "Unable to allocate Bungee PluginManager test double");
    }

    private static <T> T allocate(Class<T> type, String errorMessage) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            return type.cast(unsafe.allocateInstance(type));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(errorMessage, error);
        }
    }

    private static List<String> toList(Iterable<String> iterable) {
        List<String> values = new ArrayList<>();
        iterable.forEach(values::add);
        return values;
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
                        recordMessage(args[0]);
                        yield null;
                    }
                    case "sendMessages" -> {
                        Collections.addAll(messages, (String[]) args[0]);
                        yield null;
                    }
                    case "getGroups", "getPermissions" -> List.of();
                    case "addGroups", "removeGroups", "setPermission" -> null;
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

        private void recordMessage(Object message) {
            if (message instanceof String text) {
                messages.add(text);
                return;
            }
            if (message instanceof BaseComponent component) {
                messages.add(component.toLegacyText());
                return;
            }
            if (message instanceof BaseComponent[] components) {
                messages.add(TextComponent.toLegacyText(components));
            }
        }
    }

    private static final class RecordingRegistrar implements BungeeCommandRegistrar {
        private Plugin plugin;
        private Command registered;
        private Command unregistered;

        @Override
        public void register(Plugin plugin, Command command) {
            this.plugin = plugin;
            this.registered = command;
        }

        @Override
        public void unregister(Command command) {
            this.unregistered = command;
        }

        Plugin plugin() {
            return plugin;
        }

        Command registered() {
            return registered;
        }

        Command unregistered() {
            return unregistered;
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
        if (type == Collection.class) {
            return List.of();
        }
        if (type.isArray()) {
            return Arrays.copyOf(new Object[0], 0, (Class<? extends Object[]>) type);
        }
        return null;
    }
}
