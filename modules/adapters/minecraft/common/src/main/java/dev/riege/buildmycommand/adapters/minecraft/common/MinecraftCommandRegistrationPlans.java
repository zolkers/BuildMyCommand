package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;

import java.util.Objects;

public final class MinecraftCommandRegistrationPlans {
    private MinecraftCommandRegistrationPlans() {
    }

    public static <N> MinecraftCommandRegistrationPlan from(
        MinecraftBackendProfile backend,
        BrigadierCommandAdapter<N> bridge
    ) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(bridge, "bridge");
        return new MinecraftCommandRegistrationPlan(
            backend,
            bridge.registrationLabels().rootLabels(),
            1,
            backend.reloadSafe()
        );
    }
}
