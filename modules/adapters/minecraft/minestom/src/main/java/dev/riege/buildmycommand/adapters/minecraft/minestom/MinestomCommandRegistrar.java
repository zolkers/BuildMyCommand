package dev.riege.buildmycommand.adapters.minecraft.minestom;

public interface MinestomCommandRegistrar {
    void register(MinestomNativeCommand command);

    void unregister(MinestomNativeCommand command);
}
