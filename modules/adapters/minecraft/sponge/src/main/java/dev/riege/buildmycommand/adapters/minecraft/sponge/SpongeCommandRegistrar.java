package dev.riege.buildmycommand.adapters.minecraft.sponge;

public interface SpongeCommandRegistrar<C> {
    void register(Object pluginContainer, C command, String alias, String[] aliases);
}
