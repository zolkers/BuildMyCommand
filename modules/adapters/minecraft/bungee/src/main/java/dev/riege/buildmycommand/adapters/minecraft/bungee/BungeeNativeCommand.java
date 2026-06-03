package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;
import java.util.Objects;

public final class BungeeNativeCommand extends Command implements TabExecutor {
    private final IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter;

    public BungeeNativeCommand(
        String name,
        String[] aliases,
        IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        super(requireLabel(name), null, Objects.requireNonNull(aliases, "aliases"));
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public IAdapter<CommandSender, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return adapter;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MinecraftRenderedResult result = adapter.execute(
            Objects.requireNonNull(sender, "sender"),
            BungeeMinecraftAdapter.commandInput(getName(), Objects.requireNonNull(args, "args"))
        );
        result.message().ifPresent(sender::sendMessage);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        MinecraftInvocation invocation = BungeeMinecraftAdapter.commandInput(getName(), Objects.requireNonNull(args, "args"));
        return adapter.suggest(
            Objects.requireNonNull(sender, "sender"),
            invocation,
            invocation.cursor()
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
