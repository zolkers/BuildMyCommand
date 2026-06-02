package dev.riege.buildmycommand.adapters.minecraft.paper;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierBridge;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierRoot;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PaperBrigadierRegistration {
    private static final String MATCHING_NOTICE =
        "Paper Brigadier projection registers exact literal nodes; it does not add case-insensitive aliases.";

    private final MinecraftBackendProfile profile;
    private final MinecraftBrigadierBridge<CommandSourceStack> bridge;

    public PaperBrigadierRegistration(
        MinecraftBackendProfile profile,
        MinecraftBrigadierBridge<CommandSourceStack> bridge
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public PaperCommandRegistrationMode mode() {
        return PaperCommandRegistrationMode.BRIGADIER_PROJECTION;
    }

    public MinecraftBrigadierBridge<CommandSourceStack> bridge() {
        return bridge;
    }

    public List<LiteralCommandNode<CommandSourceStack>> roots() {
        return bridge.roots();
    }

    public List<String> labels() {
        return bridge.projectedRoots().stream()
            .flatMap(root -> root.registrationLabels().stream())
            .toList();
    }

    public List<String> rootLiterals() {
        return plan().rootLiterals();
    }

    public Set<String> register(Commands commands) {
        Objects.requireNonNull(commands, "commands");
        Set<String> registered = new LinkedHashSet<>();
        for (MinecraftBrigadierRoot<CommandSourceStack> root : bridge.projectedRoots()) {
            registered.addAll(commands.register(root.root(), root.aliases()));
        }
        return Collections.unmodifiableSet(registered);
    }

    public PaperBrigadierRegistration attachLifecycle(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin").getLifecycleManager()
            .registerEventHandler(LifecycleEvents.COMMANDS, registrationHandler());
        return this;
    }

    public LifecycleEventHandler<ReloadableRegistrarEvent<Commands>> registrationHandler() {
        return event -> register(Objects.requireNonNull(event, "event").registrar());
    }

    public MinecraftCommandRegistrationPlan plan() {
        return bridge.registrationPlan(profile);
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
