package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface NeoForgeRegisterCommandsEventView<N> {
    CommandDispatcher<N> dispatcher();
}
