/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftAdapterContracts;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BungeeMinecraftIntegration {
    private BungeeMinecraftIntegration() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.bungee();
    }

    public static MinecraftInvocation commandInput(String boundLabel, String[] args) {
        return MinecraftInvocation.labelAndArgs(boundLabel, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> commandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }

    public static CommandSource commandSource(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        return new CommandSource() {
            @Override
            public Optional<String> name() {
                return Optional.ofNullable(sender.getName());
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public <T> Optional<T> unwrap(Class<T> type) {
                return type.isInstance(sender) ? Optional.of(type.cast(sender)) : Optional.empty();
            }

            @Override
            public void reply(String message) {
                sender.sendMessage(message);
            }
        };
    }

    public static MinecraftNativeCommandAdapter<CommandSender> commandAdapter(CommandFramework framework) {
        return commandAdapter(framework, BungeeMinecraftIntegration::commandSource);
    }

    public static BungeeNativeCommand nativeCommand(
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        Objects.requireNonNull(adapter, "adapter");
        List<String> labels = MinecraftAdapterContracts.rootLabels(adapter);
        if (labels.isEmpty()) {
            throw new IllegalStateException("Bungee registration requires at least one command label");
        }
        return new BungeeNativeCommand(
            labels.get(0),
            labels.subList(1, labels.size()).toArray(String[]::new),
            adapter
        );
    }

    public static BungeeCommandRegistration registration(
        Plugin plugin,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return new BungeeCommandRegistration(plugin, adapter);
    }
}
