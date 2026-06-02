package dev.riege.buildmycommand.adapters.minecraft.bungee;

import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.Objects;

public interface BungeeCommandRegistrar {
    void register(Plugin plugin, Command command);

    void unregister(Command command);

    static BungeeCommandRegistrar pluginManager(PluginManager pluginManager) {
        Objects.requireNonNull(pluginManager, "pluginManager");
        return new BungeeCommandRegistrar() {
            @Override
            public void register(Plugin plugin, Command command) {
                pluginManager.registerCommand(plugin, command);
            }

            @Override
            public void unregister(Command command) {
                pluginManager.unregisterCommand(command);
            }
        };
    }
}
