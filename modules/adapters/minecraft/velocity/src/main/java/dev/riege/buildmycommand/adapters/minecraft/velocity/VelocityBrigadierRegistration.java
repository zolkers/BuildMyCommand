package dev.riege.buildmycommand.adapters.minecraft.velocity;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftBackendProfile;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierRoot;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlan;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftCommandRegistrationPlans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class VelocityBrigadierRegistration {
    private static final String MATCHING_NOTICE =
        "Velocity Brigadier metadata exposes aliases while BuildMyCommand delegates framework matching through _bmc_input fallbacks.";

    private final MinecraftBackendProfile profile;
    private final BrigadierCommandAdapter<com.velocitypowered.api.command.CommandSource> bridge;
    private final Object plugin;
    private final List<CommandMeta> registered = new ArrayList<>();

    public VelocityBrigadierRegistration(
        MinecraftBackendProfile profile,
        BrigadierCommandAdapter<com.velocitypowered.api.command.CommandSource> bridge,
        Object plugin
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.plugin = plugin;
    }

    public BrigadierCommandAdapter<com.velocitypowered.api.command.CommandSource> bridge() {
        return bridge;
    }

    public List<LiteralCommandNode<com.velocitypowered.api.command.CommandSource>> roots() {
        return bridge.roots();
    }

    public List<String> labels() {
        return bridge.projectedRoots().stream()
            .flatMap(root -> root.registrationLabels().stream())
            .toList();
    }

    public List<BrigadierCommand> commands() {
        return bridge.projectedRoots().stream()
            .map(BrigadierRoot::root)
            .map(BrigadierCommand::new)
            .toList();
    }

    public Set<String> register(CommandManager commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");
        unregister(commandManager);

        Set<String> labels = new LinkedHashSet<>();
        for (BrigadierRoot<com.velocitypowered.api.command.CommandSource> root : bridge.projectedRoots()) {
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
        return Collections.unmodifiableSet(labels);
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
        return MinecraftCommandRegistrationPlans.from(profile, bridge);
    }

    public boolean exactLiteralMatching() {
        return true;
    }

    public boolean frameworkAuthoritativeMatching() {
        return true;
    }

    public String matchingNotice() {
        return MATCHING_NOTICE;
    }
}
