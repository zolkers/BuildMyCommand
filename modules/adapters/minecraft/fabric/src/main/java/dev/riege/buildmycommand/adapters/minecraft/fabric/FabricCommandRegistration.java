package dev.riege.buildmycommand.adapters.minecraft.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FabricCommandRegistration<N> {
    private static final String MATCHING_NOTICE =
        "Fabric registers Brigadier literal nodes directly; aliases are redirect literals and remain exact-case.";

    private final MinecraftBackendProfile profile;
    private final BrigadierCommandAdapter<N> bridge;

    public FabricCommandRegistration(MinecraftBackendProfile profile, BrigadierCommandAdapter<N> bridge) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public BrigadierCommandAdapter<N> bridge() {
        return bridge;
    }

    public List<String> labels() {
        return bridge.registration().labels();
    }

    public Set<String> register(CommandDispatcher<N> dispatcher) {
        return bridge.registration().register(dispatcher);
    }

    public FabricCommandRegistrationCallback<N> callback() {
        return (dispatcher, registryAccess, environment) -> register(dispatcher);
    }

    public void registerFromCallback(
        CommandDispatcher<N> dispatcher,
        Object registryAccess,
        Object environment
    ) {
        callback().register(dispatcher, registryAccess, environment);
    }

    public MinecraftCommandRegistrationPlan plan() {
        return MinecraftCommandRegistrationPlans.from(profile, bridge);
    }

    public String callbackName() {
        return "CommandRegistrationCallback.EVENT";
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
