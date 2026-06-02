package dev.riege.buildmycommand.adapters.minecraft.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierBridge;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ForgeCommandRegistration<N> {
    private static final String MATCHING_NOTICE =
        "Forge RegisterCommandsEvent exposes the Minecraft Brigadier dispatcher; aliases are exact redirect literals.";

    private final MinecraftBackendProfile profile;
    private final MinecraftBrigadierBridge<N> bridge;

    public ForgeCommandRegistration(MinecraftBackendProfile profile, MinecraftBrigadierBridge<N> bridge) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public MinecraftBrigadierBridge<N> bridge() {
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

    public Set<String> register(ForgeRegisterCommandsEventView<N> event) {
        return register(Objects.requireNonNull(event, "event").dispatcher());
    }

    public MinecraftCommandRegistrationPlan plan() {
        return bridge.registrationPlan(profile);
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
