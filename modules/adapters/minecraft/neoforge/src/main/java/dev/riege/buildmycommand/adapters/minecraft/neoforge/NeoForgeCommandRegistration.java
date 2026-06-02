package dev.riege.buildmycommand.adapters.minecraft.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class NeoForgeCommandRegistration<N> {
    private static final String MATCHING_NOTICE =
        "NeoForge RegisterCommandsEvent exposes the Minecraft Brigadier dispatcher; aliases are exact redirect literals.";

    private final MinecraftBackendProfile profile;
    private final BrigadierCommandAdapter<N> bridge;

    public NeoForgeCommandRegistration(MinecraftBackendProfile profile, BrigadierCommandAdapter<N> bridge) {
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

    public Set<String> register(NeoForgeRegisterCommandsEventView<N> event) {
        return register(Objects.requireNonNull(event, "event").dispatcher());
    }

    public MinecraftCommandRegistrationPlan plan() {
        return MinecraftCommandRegistrationPlans.from(profile, bridge);
    }

    public String eventName() {
        return "RegisterCommandsEvent";
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
