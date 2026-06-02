package dev.riege.buildmycommand.adapters.minecraft.spigot;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Objects;

public final class SpigotNativeCommand extends Command implements CommandExecutor, TabCompleter {
    private final MinecraftNativeCommandAdapter<CommandSender> adapter;

    public SpigotNativeCommand(String label, MinecraftNativeCommandAdapter<CommandSender> adapter) {
        super(requireLabel(label));
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public MinecraftNativeCommandAdapter<CommandSender> adapter() {
        return adapter;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return onCommand(sender, this, commandLabel, args);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Objects.requireNonNull(command, "command");
        MinecraftRenderedResult result = adapter.execute(
            Objects.requireNonNull(sender, "sender"),
            SpigotMinecraftAdapter.commandExecutorInput(label, Objects.requireNonNull(args, "args"))
        );
        result.message().ifPresent(sender::sendMessage);
        return result.numericResult() > 0;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Objects.requireNonNull(command, "command");
        return adapter.suggest(
            Objects.requireNonNull(sender, "sender"),
            SpigotMinecraftAdapter.commandExecutorInput(alias, Objects.requireNonNull(args, "args"))
        );
    }

    private static String requireLabel(String label) {
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        return label;
    }
}
