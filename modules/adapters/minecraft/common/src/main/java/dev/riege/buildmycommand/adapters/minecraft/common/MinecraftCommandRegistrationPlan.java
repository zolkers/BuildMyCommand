package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public record MinecraftCommandRegistrationPlan(
    MinecraftBackendProfile backend,
    List<String> rootLiterals,
    long generation,
    boolean reloadSafe
) {
    private static final AtomicLong GENERATIONS = new AtomicLong();

    public MinecraftCommandRegistrationPlan {
        Objects.requireNonNull(backend, "backend");
        rootLiterals = List.copyOf(Objects.requireNonNull(rootLiterals, "rootLiterals"));
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be positive or zero");
        }
    }

    public static MinecraftCommandRegistrationPlan fromBridge(
        MinecraftBackendProfile backend,
        MinecraftCommandBridge<?> bridge
    ) {
        Objects.requireNonNull(bridge, "bridge");
        return new MinecraftCommandRegistrationPlan(
            backend,
            bridge.rootLabels(),
            GENERATIONS.incrementAndGet(),
            backend.reloadSafe()
        );
    }

    public static MinecraftCommandRegistrationPlan fromNativeAdapter(
        MinecraftBackendProfile backend,
        MinecraftNativeCommandAdapter<?> adapter
    ) {
        Objects.requireNonNull(adapter, "adapter");
        return new MinecraftCommandRegistrationPlan(
            backend,
            adapter.rootLabels(),
            GENERATIONS.incrementAndGet(),
            backend.reloadSafe()
        );
    }
}
