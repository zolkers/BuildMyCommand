package dev.riege.buildmycommand.adapters.minecraft.velocity;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierBridge;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBrigadierRoot;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class VelocityBrigadierRegistration {
    private static final String MATCHING_NOTICE =
        "Velocity command metadata treats aliases case-insensitively; Brigadier literal children remain exact.";

    private final MinecraftBackendProfile profile;
    private final MinecraftBrigadierBridge<com.velocitypowered.api.command.CommandSource> bridge;
    private final Object plugin;
    private final List<CommandMeta> registered = new ArrayList<>();

    public VelocityBrigadierRegistration(
        MinecraftBackendProfile profile,
        MinecraftBrigadierBridge<com.velocitypowered.api.command.CommandSource> bridge,
        Object plugin
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.plugin = plugin;
    }

    public MinecraftBrigadierBridge<com.velocitypowered.api.command.CommandSource> bridge() {
        return bridge;
    }

    public List<LiteralCommandNode<com.velocitypowered.api.command.CommandSource>> roots() {
        return bridge.roots();
    }

    public List<String> labels() {
        return roots().stream().map(LiteralCommandNode::getLiteral).toList();
    }

    public List<BrigadierCommand> commands() {
        return bridge.projectedRoots().stream()
            .map(MinecraftBrigadierRoot::root)
            .map(BrigadierCommand::new)
            .toList();
    }

    public Set<String> register(CommandManager commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");
        unregister(commandManager);

        Set<String> labels = new LinkedHashSet<>();
        for (MinecraftBrigadierRoot<com.velocitypowered.api.command.CommandSource> root : bridge.projectedRoots()) {
            BrigadierCommand command = new BrigadierCommand(root.root());
            String label = command.getNode().getLiteral();
            CommandMeta.Builder builder = commandManager.metaBuilder(label);
            if (!root.aliases().isEmpty()) {
                builder.aliases(root.aliases().toArray(String[]::new));
            }
            if (plugin != null) {
                builder.plugin(plugin);
            }
            CommandMeta meta = builder.build();
            commandManager.register(meta, command);
            registered.add(meta);
            labels.addAll(root.registrationLabels());
        }
        return java.util.Collections.unmodifiableSet(labels);
    }

    public VelocityBrigadierRegistration unregister(CommandManager commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");
        if (registered.isEmpty()) {
            return this;
        }
        for (CommandMeta meta : List.copyOf(registered)) {
            commandManager.unregister(meta);
        }
        registered.clear();
        return this;
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
