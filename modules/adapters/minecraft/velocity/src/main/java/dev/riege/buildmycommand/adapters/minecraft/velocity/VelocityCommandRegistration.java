package dev.riege.buildmycommand.adapters.minecraft.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;

import java.util.List;
import java.util.Objects;

public final class VelocityCommandRegistration {
    private final Object plugin;
    private final MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter;
    private CommandMeta registeredMeta;
    private VelocitySimpleCommand registeredCommand;

    public VelocityCommandRegistration(
        Object plugin,
        MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter
    ) {
        this.plugin = plugin;
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public Object plugin() {
        return plugin;
    }

    public MinecraftNativeCommandAdapter<com.velocitypowered.api.command.CommandSource> adapter() {
        return adapter;
    }

    public List<String> labels() {
        return adapter.rootLabels();
    }

    public VelocitySimpleCommand command() {
        if (registeredCommand == null) {
            registeredCommand = new VelocitySimpleCommand(adapter);
        }
        return registeredCommand;
    }

    public CommandMeta register(CommandManager commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");
        unregister(commandManager);

        List<String> labels = labels();
        if (labels.isEmpty()) {
            throw new IllegalStateException("Velocity registration requires at least one command label");
        }

        CommandMeta.Builder builder = commandManager.metaBuilder(labels.get(0));
        if (labels.size() > 1) {
            builder.aliases(labels.subList(1, labels.size()).toArray(String[]::new));
        }
        if (plugin != null) {
            builder.plugin(plugin);
        }
        CommandMeta meta = builder.build();
        commandManager.register(meta, command());
        registeredMeta = meta;
        return meta;
    }

    public VelocityCommandRegistration unregister(CommandManager commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");
        if (registeredMeta != null) {
            commandManager.unregister(registeredMeta);
            registeredMeta = null;
        }
        return this;
    }
}
