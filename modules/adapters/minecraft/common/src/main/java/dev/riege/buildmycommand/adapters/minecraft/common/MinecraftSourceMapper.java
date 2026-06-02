package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandSource;

@FunctionalInterface
public interface MinecraftSourceMapper<S> {
    CommandSource map(S source);
}
