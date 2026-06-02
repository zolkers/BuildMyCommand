package dev.riege.buildmycommand.adapters.minecraft.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.LinkedHashSet;
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
        return bridge.roots().stream().map(LiteralCommandNode::getLiteral).toList();
    }

    public Set<String> register(CommandDispatcher<N> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Set<String> registered = new LinkedHashSet<>();
        for (LiteralCommandNode<N> root : bridge.roots()) {
            dispatcher.getRoot().addChild(root);
            registered.add(root.getLiteral());
        }
        return java.util.Collections.unmodifiableSet(registered);
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
