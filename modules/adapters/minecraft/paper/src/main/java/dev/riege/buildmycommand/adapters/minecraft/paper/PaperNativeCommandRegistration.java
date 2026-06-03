package dev.riege.buildmycommand.adapters.minecraft.paper;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.minecraft.spigot.SpigotCommandRegistration;
import dev.riege.buildmycommand.adapters.minecraft.spigot.SpigotNativeCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public final class PaperNativeCommandRegistration {
    private final SpigotCommandRegistration spigotFallback;

    public PaperNativeCommandRegistration(
        String fallbackPrefix,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        this(new SpigotCommandRegistration(fallbackPrefix, adapter));
    }

    public PaperNativeCommandRegistration(SpigotCommandRegistration spigotFallback) {
        this.spigotFallback = Objects.requireNonNull(spigotFallback, "spigotFallback");
    }

    public PaperCommandRegistrationMode mode() {
        return PaperCommandRegistrationMode.NATIVE_COMMAND;
    }

    public String fallbackPrefix() {
        return spigotFallback.fallbackPrefix();
    }

    public IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return spigotFallback.adapter();
    }

    public List<String> labels() {
        return spigotFallback.labels();
    }

    public List<SpigotNativeCommand> commands() {
        return spigotFallback.commands();
    }

    public List<String> register(CommandMap commandMap) {
        return spigotFallback.register(commandMap);
    }

    public PaperNativeCommandRegistration unregister(CommandMap commandMap) {
        spigotFallback.unregister(commandMap);
        return this;
    }

    public SpigotCommandRegistration spigotFallback() {
        return spigotFallback;
    }
}
