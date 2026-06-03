package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftAdapterContracts;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class MinestomMinecraftIntegration {
    private MinestomMinecraftIntegration() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.minestom();
    }

    public static MinecraftInvocation commandInput(String label, String[] args) {
        return MinecraftInvocation.labelAndArgs(label, args, Math.max(0, args.length - 1));
    }

    public static CommandSource commandSource(Object sender) {
        Objects.requireNonNull(sender, "sender");
        return new CommandSource() {
            @Override
            public <T> Optional<T> unwrap(Class<T> type) {
                return type.isInstance(sender) ? Optional.of(type.cast(sender)) : Optional.empty();
            }

            @Override
            public void reply(String message) {
                invokeSendMessage(sender, message);
            }
        };
    }

    public static MinecraftNativeCommandAdapter<Object> commandAdapter(
        CommandFramework framework
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, MinestomMinecraftIntegration::commandSource);
    }

    public static BrigadierCommandAdapter<Object> brigadierBridge(CommandFramework framework) {
        return MinecraftBrigadierAdapters.create(profile(), framework, MinestomMinecraftIntegration::commandSource);
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper);
    }

    public static MinestomNativeCommand nativeCommand(
        IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        Objects.requireNonNull(adapter, "adapter");
        List<String> labels = MinecraftAdapterContracts.rootLabels(adapter);
        if (labels.isEmpty()) {
            throw new IllegalStateException("Minestom registration requires at least one command label");
        }
        return new MinestomNativeCommand(
            labels.get(0),
            labels.subList(1, labels.size()).toArray(String[]::new),
            adapter
        );
    }

    public static MinestomCommandRegistration registration(
        IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return new MinestomCommandRegistration(profile(), adapter);
    }

    private static void invokeSendMessage(Object sender, String message) {
        try {
            sender.getClass().getMethod("sendMessage", String.class).invoke(sender, message);
        } catch (ReflectiveOperationException ignored) {
            // Minestom versions differ between String and Component sendMessage overloads.
        }
    }
}
