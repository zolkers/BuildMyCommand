package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierRegistration;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.Objects;

public record NeoForgeBrigadierRegistration<N>(
    MinecraftBackendProfile backend,
    NeoForgeCommandApi api,
    BrigadierCommandAdapter<N> bridge
) {
    public NeoForgeBrigadierRegistration {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(bridge, "bridge");
    }

    public BrigadierRegistration<N> registerInto(CommandDispatcher<N> dispatcher) {
        BrigadierRegistration<N> registration = bridge.registration();
        registration.register(dispatcher);
        return registration;
    }

    public MinecraftCommandRegistrationPlan plan() {
        return MinecraftCommandRegistrationPlans.from(backend, bridge);
    }
}
