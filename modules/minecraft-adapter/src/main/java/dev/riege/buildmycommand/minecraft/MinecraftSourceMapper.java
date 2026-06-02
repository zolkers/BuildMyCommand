package dev.riege.buildmycommand.minecraft;

import dev.riege.buildmycommand.api.CommandSource;

@FunctionalInterface
public interface MinecraftSourceMapper<S> {
    CommandSource map(S source);
}
