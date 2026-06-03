package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;
import org.bukkit.command.CommandSender;

public final class SpigotMinecraftAdapter {
    private SpigotMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.spigot();
    }

    public static MinecraftInvocation commandExecutorInput(String label, String[] args) {
        return MinecraftInvocation.labelAndArgs(label, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> commandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }

    public static CommandSource commandSource(CommandSender sender) {
        return new SpigotCommandSource(sender);
    }

    public static SpigotNativeCommand nativeCommand(
        String label,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return new SpigotNativeCommand(label, adapter);
    }

    public static SpigotCommandRegistration registration(
        String fallbackPrefix,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return new SpigotCommandRegistration(fallbackPrefix, adapter);
    }
}
