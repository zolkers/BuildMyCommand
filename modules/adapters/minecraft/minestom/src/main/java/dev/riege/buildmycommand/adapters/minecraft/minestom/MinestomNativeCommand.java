package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.IAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftInvocation;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MinestomNativeCommand {
    private final String name;
    private final String[] aliases;
    private final IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter;

    public MinestomNativeCommand(
        String name,
        String[] aliases,
        IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter
    ) {
        this.name = requireLabel(name);
        this.aliases = Objects.requireNonNull(aliases, "aliases").clone();
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public IAdapter<Object, MinecraftInvocation, MinecraftRenderedResult> adapter() {
        return adapter;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases.clone();
    }

    public void execute(Object sender, String[] args) {
        MinecraftRenderedResult result = adapter.execute(
            Objects.requireNonNull(sender, "sender"),
            MinestomMinecraftIntegration.commandInput(name, Objects.requireNonNull(args, "args"))
        );
        result.message().ifPresent(message -> MinestomMinecraftIntegration.commandSource(sender).reply(message));
    }

    public List<String> suggest(Object sender, String[] args) {
        MinecraftInvocation invocation =
            MinestomMinecraftIntegration.commandInput(name, Objects.requireNonNull(args, "args"));
        return adapter.suggest(
            Objects.requireNonNull(sender, "sender"),
            invocation,
            invocation.cursor()
        );
    }

    public List<String> aliases() {
        return Arrays.asList(getAliases());
    }

    private static String requireLabel(String label) {
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        return label;
    }
}
