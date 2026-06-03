package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierAdapters;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfiles;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.adapters.minecraft.spigot.SpigotMinecraftAdapter;
import dev.riege.buildmycommand.adapters.minecraft.spigot.SpigotNativeCommand;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

public final class PaperMinecraftAdapter {
    private PaperMinecraftAdapter() {
    }

    public static MinecraftBackendProfile profile() {
        return MinecraftBackendProfiles.paper();
    }

    public static MinecraftInvocation brigadierInput(String input, int cursor) {
        return MinecraftInvocation.slash(input, cursor);
    }

    public static MinecraftInvocation commandInput(String label, String[] args) {
        return MinecraftInvocation.labelAndArgs(label, args, Math.max(0, args.length - 1));
    }

    public static <S> MinecraftNativeCommandAdapter<S> commandAdapter(
        CommandFramework framework,
        MinecraftSourceMapper<S> sourceMapper
    ) {
        return new MinecraftNativeCommandAdapter<>(framework, sourceMapper);
    }

    public static CommandSource commandSource(CommandSender sender) {
        return SpigotMinecraftAdapter.commandSource(sender);
    }

    public static PaperCommandRegistrationStrategy strategy(PaperCommandRegistrationMode mode) {
        return PaperCommandRegistrationStrategy.of(mode);
    }

    public static <N> BrigadierCommandAdapter<N> brigadierBridge(
        CommandFramework framework,
        MinecraftSourceMapper<N> sourceMapper
    ) {
        return MinecraftBrigadierAdapters.create(profile(), framework, sourceMapper::map);
    }

    public static PaperBrigadierRegistration brigadierRegistration(
        CommandFramework framework,
        MinecraftSourceMapper<CommandSourceStack> sourceMapper
    ) {
        return new PaperBrigadierRegistration(profile(), brigadierBridge(framework, sourceMapper));
    }

    public static SpigotNativeCommand nativeCommand(
        String label,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return SpigotMinecraftAdapter.nativeCommand(label, adapter);
    }

    public static PaperNativeCommandRegistration nativeRegistration(
        String fallbackPrefix,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        return new PaperNativeCommandRegistration(fallbackPrefix, adapter);
    }
}
