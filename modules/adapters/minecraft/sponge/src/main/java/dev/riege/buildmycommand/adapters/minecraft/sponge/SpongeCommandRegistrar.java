package dev.riege.buildmycommand.adapters.minecraft.sponge;

import java.util.Objects;

public interface SpongeCommandRegistrar<C> {
    void register(Object pluginContainer, C command, String alias, String[] aliases);
}
