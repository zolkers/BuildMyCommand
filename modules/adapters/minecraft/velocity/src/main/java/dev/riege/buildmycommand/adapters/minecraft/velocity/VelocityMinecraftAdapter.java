package dev.riege.buildmycommand.adapters.minecraft.velocity;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Objects;

public final class VelocityMinecraftAdapter {
    private VelocityMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.velocity();
    }

    public static MinecraftInvocation simpleCommandInput(String alias, String[] args) {
        return MinecraftInvocation.labelAndArgs(alias, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> simpleCommandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }

    public static CommandSource commandSource(com.velocitypowered.api.command.CommandSource source) {
        Objects.requireNonNull(source, "source");
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return source.hasPermission(permission);
            }

            @Override
            public <T> java.util.Optional<T> unwrap(Class<T> type) {
                return type.isInstance(source) ? java.util.Optional.of(type.cast(source)) : java.util.Optional.empty();
            }

            @Override
            public void reply(String message) {
                source.sendMessage(net.kyori.adventure.text.Component.text(message));
            }
        };
    }

    public static MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> simpleCommandAdapter(
        CommandFramework framework
    ) {
        return simpleCommandAdapter(framework, VelocityMinecraftAdapter::commandSource);
    }

    public static VelocitySimpleCommand simpleCommand(
        MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter
    ) {
        return new VelocitySimpleCommand(adapter);
    }

    public static VelocityCommandRegistration simpleRegistration(
        Object plugin,
        MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter
    ) {
        return new VelocityCommandRegistration(plugin, adapter);
    }

    public static BrigadierCommandAdapter<com.velocitypowered.api.command.CommandSource> brigadierBridge(
        CommandFramework framework
    ) {
        return MinecraftBrigadierAdapters.create(framework, VelocityMinecraftAdapter::commandSource);
    }

    public static VelocityBrigadierRegistration brigadierRegistration(
        CommandFramework framework,
        Object plugin
    ) {
        return new VelocityBrigadierRegistration(profile(), brigadierBridge(framework), plugin);
    }
}
