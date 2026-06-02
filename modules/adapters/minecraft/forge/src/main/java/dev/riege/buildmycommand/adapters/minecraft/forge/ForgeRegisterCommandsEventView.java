package dev.riege.buildmycommand.adapters.minecraft.forge;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface ForgeRegisterCommandsEventView<N> {
    CommandDispatcher<N> dispatcher();
}
