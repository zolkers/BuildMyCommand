package dev.riege.buildmycommand.adapters.minecraft.minestom;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;

import java.util.Arrays;
import java.util.Objects;

public final class MinestomNativeCommand {
    private final String name;
    private final String[] aliases;
    private final MinecraftNativeCommandAdapter<Object> adapter;

    public MinestomNativeCommand(
        String name,
        String[] aliases,
        MinecraftNativeCommandAdapter<Object> adapter
    ) {
        this.name = requireLabel(name);
        this.aliases = Objects.requireNonNull(aliases, "aliases").clone();
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public MinecraftNativeCommandAdapter<Object> adapter() {
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
            MinestomMinecraftAdapter.commandInput(name, Objects.requireNonNull(args, "args"))
        );
        result.message().ifPresent(message -> MinestomMinecraftAdapter.commandSource(sender).reply(message));
    }

    public java.util.List<String> suggest(Object sender, String[] args) {
        return adapter.suggest(
            Objects.requireNonNull(sender, "sender"),
            MinestomMinecraftAdapter.commandInput(name, Objects.requireNonNull(args, "args"))
        );
    }

    public java.util.List<String> aliases() {
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
