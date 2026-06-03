package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterMatchingPolicy;
import dev.riege.buildmycommand.adapters.AdapterRegistrationLabels;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCapability;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftResultRenderer;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinestomMinecraftAdapterTest {
    @Test
    void exposesMinestomCommandManagerProfile() {
        assertTrue(MinestomMinecraftAdapter.profile().capabilities().contains(MinecraftCapability.BRIGADIER));
        assertEquals("minestom reload", MinestomMinecraftAdapter.commandInput("minestom", new String[] {"reload"}).normalizedInput());
    }

    @Test
    void createsNativeCommandWithAliasesAndRegistrationPlan() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms reload").executes(ctx -> Results.silent());

        MinestomCommandRegistration registration = MinestomMinecraftAdapter.registration(
            MinestomMinecraftAdapter.commandAdapter(framework)
        );
        MinestomNativeCommand command = registration.command();

        assertEquals("minestom", command.getName());
        assertArrayEquals(new String[] {"ms"}, command.getAliases());
        assertEquals(List.of("minestom", "ms"), registration.labels());
        assertSame(registration.adapter(), command.adapter());
        assertEquals(List.of("minestom", "ms"), registration.plan().rootLiterals());
        assertTrue(registration.exactLiteralMatching());
        assertEquals(
            "Minestom command names and aliases are registered through Command(name, aliases...) and are exact-case.",
            registration.matchingNotice()
        );
        assertSame(command, registration.command());
        String[] aliases = command.getAliases();
        aliases[0] = "changed";
        assertArrayEquals(new String[] {"ms"}, command.getAliases());
        assertEquals(List.of("ms"), command.aliases());
    }

    @Test
    void exposesSharedBrigadierBridgeForCommandManagerIntegrations() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms reload").executes(ctx -> Results.silent());

        BrigadierCommandAdapter<Object> bridge = MinestomMinecraftAdapter.brigadierBridge(framework);
        BrigadierCommandAdapter<String> customBridge = MinestomMinecraftAdapter.brigadierBridge(framework, sender -> new CommandSource() {
        });

        assertEquals("minecraft-minestom-brigadier", bridge.config().adapterId());
        assertEquals("minecraft-minestom-brigadier", customBridge.config().adapterId());
        assertEquals(List.of("minestom", "ms"), bridge.registrationLabels().rootLabels());
    }

    @Test
    void bridgesNativeCommandExecutionSuggestionAndCommandSourceReply() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom reload").executes(ctx -> Results.success("done"));
        MinestomNativeCommand command = MinestomMinecraftAdapter.nativeCommand(
            MinestomMinecraftAdapter.commandAdapter(framework)
        );
        MessageSender sender = new MessageSender();

        command.execute(sender, new String[] {"reload"});

        assertEquals(List.of("done"), sender.messages());
        assertEquals(List.of("reload"), command.suggest(sender, new String[] {"r"}));
        assertEquals(Optional.of(sender), MinestomMinecraftAdapter.commandSource(sender).unwrap(MessageSender.class));
        assertEquals(Optional.empty(), MinestomMinecraftAdapter.commandSource(sender).unwrap(String.class));
        MinestomMinecraftAdapter.commandSource(new Object()).reply("ignored");
    }

    @Test
    void rejectsInvalidNativeCommandAndRegistrationInputs() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom").executes(ctx -> Results.silent());
        var adapter = MinestomMinecraftAdapter.commandAdapter(framework);
        MinestomNativeCommand command = new MinestomNativeCommand("minestom", new String[] {}, adapter);

        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.commandInput("minestom", null));
        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.commandSource(null));
        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.nativeCommand(null));
        assertThrows(IllegalStateException.class, () -> MinestomMinecraftAdapter.nativeCommand(new EmptyLabelsAdapter()));
        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.registration(null));
        assertThrows(NullPointerException.class, () -> new MinestomCommandRegistration(null, adapter));
        assertThrows(NullPointerException.class, () -> new MinestomCommandRegistration(MinestomMinecraftAdapter.profile(), null));
        assertThrows(NullPointerException.class, () -> new MinestomNativeCommand(null, new String[] {}, adapter));
        assertThrows(IllegalArgumentException.class, () -> new MinestomNativeCommand(" ", new String[] {}, adapter));
        assertThrows(NullPointerException.class, () -> new MinestomNativeCommand("minestom", null, adapter));
        assertThrows(NullPointerException.class, () -> new MinestomNativeCommand("minestom", new String[] {}, null));
        assertThrows(NullPointerException.class, () -> command.execute(null, new String[] {}));
        assertThrows(NullPointerException.class, () -> command.execute(new Object(), null));
        assertThrows(NullPointerException.class, () -> command.suggest(null, new String[] {}));
        assertThrows(NullPointerException.class, () -> command.suggest(new Object(), null));
        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.registration(adapter).register(null));
        assertThrows(NullPointerException.class, () -> MinestomMinecraftAdapter.registration(adapter).unregister(null));
    }

    @Test
    void registrationDelegatesToMinestomRegistrarBoundary() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("minestom|ms reload").executes(ctx -> Results.silent());
        RecordingRegistrar registrar = new RecordingRegistrar();

        MinestomCommandRegistration registration = MinestomMinecraftAdapter.registration(
            MinestomMinecraftAdapter.commandAdapter(framework)
        );

        assertSame(registration.command(), registration.register(registrar));
        assertSame(registration.command(), registrar.registered());
        registration.unregister(registrar);
        assertSame(registration.command(), registrar.unregistered());
    }

    private static final class RecordingRegistrar implements MinestomCommandRegistrar {
        private MinestomNativeCommand registered;
        private MinestomNativeCommand unregistered;

        @Override
        public void register(MinestomNativeCommand command) {
            registered = command;
        }

        @Override
        public void unregister(MinestomNativeCommand command) {
            unregistered = command;
        }

        MinestomNativeCommand registered() {
            return registered;
        }

        MinestomNativeCommand unregistered() {
            return unregistered;
        }
    }

    public static final class MessageSender {
        private final java.util.ArrayList<String> messages = new java.util.ArrayList<>();

        public void sendMessage(String message) {
            messages.add(message);
        }

        List<String> messages() {
            return List.copyOf(messages);
        }
    }

    private static final class EmptyLabelsAdapter implements IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> {
        @Override
        public AdapterRuntime runtime() {
            return new AdapterRuntime(CommandFramework.create(), CommandPlatform.test());
        }

        @Override
        public AdapterConfig config() {
            return AdapterConfig.of("empty", "Empty", new AdapterCapabilities(false, true, true));
        }

        @Override
        public AdapterCapabilities capabilities() {
            return config().capabilities();
        }

        @Override
        public AdapterRegistrationLabels registrationLabels() {
            return new AdapterRegistrationLabels(List.of(), List.of());
        }

        @Override
        public AdapterMatchingPolicy matchingPolicy() {
            return AdapterMatchingPolicy.strict();
        }

        @Override
        public AdapterRenderer<MinecraftRenderedResult> renderer() {
            return MinecraftResultRenderer.defaultRenderer()::render;
        }

        @Override
        public CommandSource mapSource(Object nativeSource) {
            return new CommandSource() {
            };
        }

        @Override
        public CommandInput mapInput(Object nativeSource, MinecraftInvocation nativeInput) {
            return CommandInput.raw(mapSource(nativeSource), nativeInput.normalizedInput());
        }

        @Override
        public CommandResult dispatch(Object nativeSource, MinecraftInvocation nativeInput) {
            return Results.silent();
        }

        @Override
        public MinecraftRenderedResult render(CommandResult result) {
            return renderer().render(result);
        }

        @Override
        public MinecraftRenderedResult execute(Object nativeSource, MinecraftInvocation nativeInput) {
            return render(dispatch(nativeSource, nativeInput));
        }

        @Override
        public List<String> suggest(Object nativeSource, MinecraftInvocation nativeInput, int cursor) {
            return List.of();
        }

        @Override
        public List<Suggestion> suggestRich(Object nativeSource, MinecraftInvocation nativeInput, int cursor) {
            return List.of();
        }
    }
}
