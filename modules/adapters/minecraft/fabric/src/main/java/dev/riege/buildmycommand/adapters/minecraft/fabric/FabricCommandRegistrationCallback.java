package dev.riege.buildmycommand.adapters.minecraft.fabric;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface FabricCommandRegistrationCallback<N> {
    void register(CommandDispatcher<N> dispatcher, Object registryAccess, Object environment);
}
