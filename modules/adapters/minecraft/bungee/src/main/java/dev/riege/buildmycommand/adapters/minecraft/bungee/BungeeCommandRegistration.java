package dev.riege.buildmycommand.adapters.minecraft.bungee;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftNativeCommandAdapter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.List;
import java.util.Objects;

public final class BungeeCommandRegistration {
    private final Plugin plugin;
    private final MinecraftNativeCommandAdapter<CommandSender> adapter;
    private BungeeNativeCommand registeredCommand;

    public BungeeCommandRegistration(Plugin plugin, MinecraftNativeCommandAdapter<CommandSender> adapter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public Plugin plugin() {
        return plugin;
    }

    public MinecraftNativeCommandAdapter<CommandSender> adapter() {
        return adapter;
    }

    public List<String> labels() {
        return adapter.registrationLabels();
    }

    public BungeeNativeCommand command() {
        if (registeredCommand == null) {
            registeredCommand = BungeeMinecraftAdapter.nativeCommand(adapter);
        }
        return registeredCommand;
    }

    public BungeeNativeCommand register(PluginManager pluginManager) {
        return register(BungeeCommandRegistrar.pluginManager(pluginManager));
    }

    public BungeeNativeCommand register(BungeeCommandRegistrar registrar) {
        Objects.requireNonNull(registrar, "registrar");
        unregister(registrar);
        registrar.register(plugin, command());
        return registeredCommand;
    }

    public BungeeCommandRegistration unregister(PluginManager pluginManager) {
        return unregister(BungeeCommandRegistrar.pluginManager(pluginManager));
    }

    public BungeeCommandRegistration unregister(BungeeCommandRegistrar registrar) {
        Objects.requireNonNull(registrar, "registrar");
        if (registeredCommand != null) {
            registrar.unregister(registeredCommand);
        }
        return this;
    }
}
