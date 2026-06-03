package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityMinecraftAdapterTest {
    @Test
    void supportsProxyBrigadierCommands() {
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.PROXY_COMMANDS));
        assertTrue(VelocityMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertEquals("proxy reload", VelocityMinecraftAdapter.simpleCommandInput("proxy", new String[] {"reload"}).normalizedInput());
    }

    @Test
    void createsNativeCommandAdapterForSimpleCommandBackends() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload").executes(ctx -> Results.silent());

        MinecraftNativeCommandAdapter<Object> adapter = VelocityMinecraftAdapter.simpleCommandAdapter(
            framework,
            sender -> new CommandSource() {
            }
        );

        assertEquals(List.of("proxy", "px"), adapter.rootLabels());
    }

    @Test
    void mapsVelocityCommandSourcePermissionsAndReplies() {
        RecordingVelocitySource source = new RecordingVelocitySource("proxy.reload");

        CommandSource mapped = VelocityMinecraftAdapter.commandSource(source.proxy());

        assertTrue(mapped.hasPermission("proxy.reload"));
        assertEquals(Optional.of(source.proxy()), mapped.unwrap(com.velocitypowered.api.command.CommandSource.class));
        mapped.reply("Reloaded");
        assertEquals(List.of("Reloaded"), source.messages());
    }

    @Test
    void simpleCommandExecutesRendersAndSuggestsAsync() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload").executes(ctx -> Results.success("Proxy reloaded"));
        framework.registry().route("proxy|px status").executes(ctx -> Results.silent());
        RecordingVelocitySource source = new RecordingVelocitySource();
        VelocitySimpleCommand command = VelocityMinecraftAdapter.simpleCommand(
            VelocityMinecraftAdapter.simpleCommandAdapter(framework)
        );

        command.execute(invocation(source.proxy(), "px", "reload"));

        assertEquals(List.of("Proxy reloaded"), source.messages());
        assertEquals(List.of("status"), command.suggest(invocation(source.proxy(), "proxy", "s")));
        assertEquals(List.of("status"), command.suggestAsync(invocation(source.proxy(), "proxy", "s")).join());
    }

    @Test
    void simpleCommandHasPermissionDelegatesToRoutePermissionForAliases() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload")
            .permission("proxy.reload")
            .executes(ctx -> Results.silent());
        VelocitySimpleCommand command = VelocityMinecraftAdapter.simpleCommand(
            VelocityMinecraftAdapter.simpleCommandAdapter(framework)
        );

        assertTrue(command.hasPermission(invocation(new RecordingVelocitySource("proxy.reload").proxy(), "px")));
        assertFalse(command.hasPermission(invocation(new RecordingVelocitySource().proxy(), "px")));
    }

    @Test
    void simpleRegistrationBuildsCommandMetaWithAliasesAndPlugin() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload").executes(ctx -> Results.silent());
        RecordingCommandManager commandManager = new RecordingCommandManager();
        Object plugin = new Object();

        VelocityCommandRegistration registration = VelocityMinecraftAdapter.simpleRegistration(
            plugin,
            VelocityMinecraftAdapter.simpleCommandAdapter(framework)
        );

        CommandMeta meta = registration.register(commandManager.proxy());

        assertEquals(List.of("proxy", "px"), registration.labels());
        assertSame(plugin, commandManager.plugin(meta));
        assertSame(registration.command(), commandManager.registered(meta));
        assertEquals(List.of("proxy", "px"), commandManager.labels(meta));
        registration.unregister(commandManager.proxy());
        assertTrue(commandManager.registrations().isEmpty());
    }

    @Test
    void brigadierRegistrationRegistersProjectedRootAndAliasNodes() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("proxy|px reload").executes(ctx -> Results.silent());
        RecordingCommandManager commandManager = new RecordingCommandManager();

        VelocityBrigadierRegistration registration = VelocityMinecraftAdapter.brigadierRegistration(
            framework,
            null
        );
        MinecraftCommandRegistrationPlan plan = registration.plan();

        assertEquals(List.of("proxy"), plan.rootLiterals());
        assertEquals(List.of("proxy", "px"), registration.labels());
        assertEquals(List.of("proxy", "px"), new ArrayList<>(registration.register(commandManager.proxy())));
        assertTrue(registration.exactLiteralMatching());
        assertEquals(1, commandManager.registrations().size());
        assertEquals(List.of("proxy", "px"), commandManager.labels(commandManager.registrations().keySet().iterator().next()));
        registration.unregister(commandManager.proxy());
        assertTrue(commandManager.registrations().isEmpty());
    }

    private static SimpleCommand.Invocation invocation(
        com.velocitypowered.api.command.CommandSource source,
        String alias,
        String... arguments
    ) {
        return (SimpleCommand.Invocation) Proxy.newProxyInstance(
            SimpleCommand.Invocation.class.getClassLoader(),
            new Class<?>[] {SimpleCommand.Invocation.class},
            (ignored, method, args) -> switch (method.getName()) {
                case "source" -> source;
                case "alias" -> alias;
                case "arguments" -> arguments;
                case "toString" -> "Invocation[" + alias + "]";
                default -> defaultReturn(method.getReturnType());
            }
        );
    }

    private static final class RecordingVelocitySource {
        private final List<String> permissions;
        private final List<String> messages = new ArrayList<>();
        private final com.velocitypowered.api.command.CommandSource proxy;

        RecordingVelocitySource(String... permissions) {
            this.permissions = List.of(permissions);
            this.proxy = (com.velocitypowered.api.command.CommandSource) Proxy.newProxyInstance(
                com.velocitypowered.api.command.CommandSource.class.getClassLoader(),
                new Class<?>[] {com.velocitypowered.api.command.CommandSource.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> this.permissions.contains((String) args[0]);
                    case "sendMessage" -> {
                        messages.add(PlainTextComponentSerializer.plainText().serialize((Component) args[0]));
                        yield null;
                    }
                    case "toString" -> "RecordingVelocitySource";
                    default -> defaultReturn(method.getReturnType());
                }
            );
        }

        com.velocitypowered.api.command.CommandSource proxy() {
            return proxy;
        }

        List<String> messages() {
            return messages;
        }
    }

    private static final class RecordingCommandManager {
        private final Map<CommandMeta, Object> registrations = new LinkedHashMap<>();
        private final Map<CommandMeta, List<String>> labels = new LinkedHashMap<>();
        private final Map<CommandMeta, Object> plugins = new LinkedHashMap<>();
        private final CommandManager proxy = (CommandManager) Proxy.newProxyInstance(
            CommandManager.class.getClassLoader(),
            new Class<?>[] {CommandManager.class},
            (ignored, method, args) -> switch (method.getName()) {
                case "metaBuilder" -> metaBuilder(args[0]);
                case "register" -> {
                    registrations.put((CommandMeta) args[0], args[1]);
                    yield null;
                }
                case "unregister" -> {
                    registrations.remove((CommandMeta) args[0]);
                    labels.remove((CommandMeta) args[0]);
                    plugins.remove((CommandMeta) args[0]);
                    yield null;
                }
                case "toString" -> "RecordingCommandManager";
                default -> defaultReturn(method.getReturnType());
            }
        );

        CommandManager proxy() {
            return proxy;
        }

        Map<CommandMeta, Object> registrations() {
            return registrations;
        }

        Object registered(CommandMeta meta) {
            return registrations.get(meta);
        }

        Object plugin(CommandMeta meta) {
            return plugins.get(meta);
        }

        List<String> labels(CommandMeta meta) {
            return labels.get(meta);
        }

        private CommandMeta.Builder metaBuilder(Object first) {
            return (CommandMeta.Builder) Proxy.newProxyInstance(
                CommandMeta.Builder.class.getClassLoader(),
                new Class<?>[] {CommandMeta.Builder.class},
                new Object() {
                    private final List<String> builderLabels = new ArrayList<>(initialLabels(first));
                    private Object builderPlugin;

                    Object invoke(Object ignored, java.lang.reflect.Method method, Object[] args) {
                        return switch (method.getName()) {
                            case "aliases" -> {
                                if (args[0] instanceof String[] aliases) {
                                    builderLabels.addAll(List.of(aliases));
                                }
                                yield ignored;
                            }
                            case "plugin" -> {
                                builderPlugin = args[0];
                                yield ignored;
                            }
                            case "build" -> buildMeta(builderLabels, builderPlugin);
                            case "toString" -> "RecordingCommandMetaBuilder";
                            default -> defaultReturn(method.getReturnType());
                        };
                    }
                }::invoke
            );
        }

        private CommandMeta buildMeta(List<String> builderLabels, Object builderPlugin) {
            CommandMeta meta = (CommandMeta) Proxy.newProxyInstance(
                CommandMeta.class.getClassLoader(),
                new Class<?>[] {CommandMeta.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "getAliases" -> builderLabels;
                    case "toString" -> "CommandMeta" + builderLabels;
                    default -> defaultReturn(method.getReturnType());
                }
            );
            labels.put(meta, List.copyOf(builderLabels));
            plugins.put(meta, builderPlugin);
            return meta;
        }

        private static List<String> initialLabels(Object first) {
            if (first instanceof String label) {
                return List.of(label);
            }
            if (first instanceof com.velocitypowered.api.command.BrigadierCommand command) {
                return List.of(command.getNode().getLiteral());
            }
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
