package dev.riege.buildmycommand.adapters.minecraft.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class MinecraftBrigadierBridge<N> {
    private static final String FLAG_TUNNEL = "_bmc_flags";

    private final CommandFramework framework;
    private final Function<N, CommandSource> sourceMapper;
    private final CommandPlatform platform;

    private MinecraftBrigadierBridge(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper,
        CommandPlatform platform
    ) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    public static <N> MinecraftBrigadierBridge<N> create(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        return new MinecraftBrigadierBridge<>(framework, sourceMapper,
            new CommandPlatform("minecraft", "Minecraft", false, true, true));
    }

    public List<LiteralCommandNode<N>> roots() {
        List<LiteralCommandNode<N>> roots = new ArrayList<>();
        for (CommandNode root : framework.graph().roots()) {
            LiteralCommandNode<N> rootNode = convertRoot(root).build();
            roots.add(rootNode);
            for (String alias : root.aliases()) {
                roots.add(LiteralArgumentBuilder.<N>literal(alias)
                    .requires(rootNode.getRequirement())
                    .executes(rootNode.getCommand())
                    .redirect(rootNode)
                    .build());
            }
        }
        return List.copyOf(roots);
    }

    public MinecraftCommandRegistrationPlan registrationPlan(MinecraftBackendProfile backend) {
        Objects.requireNonNull(backend, "backend");
        return new MinecraftCommandRegistrationPlan(
            backend,
            framework.graph().roots().stream().map(CommandNode::literal).toList(),
            1,
            backend.reloadSafe()
        );
    }

    private LiteralArgumentBuilder<N> convertRoot(CommandNode node) {
        LiteralArgumentBuilder<N> builder = LiteralArgumentBuilder.<N>literal(node.literal())
            .requires(nativeSource -> canAccess(nativeSource, node));
        attachExecutor(builder);
        attachNodeContents(builder, node);
        return builder;
    }

    private com.mojang.brigadier.tree.CommandNode<N> convertChild(CommandNode node) {
        LiteralArgumentBuilder<N> builder = LiteralArgumentBuilder.<N>literal(node.literal())
            .requires(nativeSource -> canAccess(nativeSource, node));
        attachExecutor(builder);
        attachNodeContents(builder, node);
        com.mojang.brigadier.tree.LiteralCommandNode<N> built = builder.build();
        for (String alias : node.aliases()) {
            built.addChild(LiteralArgumentBuilder.<N>literal(alias)
                .requires(built.getRequirement())
                .executes(built.getCommand())
                .redirect(built)
                .build());
        }
        return built;
    }

    private void attachNodeContents(ArgumentBuilder<N, ?> builder, CommandNode node) {
        if (!node.arguments().isEmpty()) {
            builder.then(argumentBranch(node, 0));
            return;
        }
        attachTerminalContents(builder, node);
    }

    private RequiredArgumentBuilder<N, ?> argumentBranch(CommandNode node, int index) {
        ArgumentSpec<?> argument = node.arguments().get(index);
        RequiredArgumentBuilder<N, ?> builder = RequiredArgumentBuilder.argument(
            argument.name(),
            brigadierArgument(argument)
        );
        builder.suggests((context, suggestionsBuilder) -> {
            CommandSource source = sourceMapper.apply(context.getSource());
            CommandInput input = input(source, context.getInput(), context.getInput().length());
            for (dev.riege.buildmycommand.api.Suggestion suggestion : framework.suggestRich(input)) {
                suggestionsBuilder.suggest(suggestion.value(),
                    suggestion.tooltip().map(LiteralMessage::new).orElse(null));
            }
            return suggestionsBuilder.buildFuture();
        });
        attachExecutor(builder);
        if (index + 1 < node.arguments().size()) {
            builder.then(argumentBranch(node, index + 1));
        } else {
            attachTerminalContents(builder, node);
        }
        return builder;
    }

    private void attachTerminalContents(ArgumentBuilder<N, ?> builder, CommandNode node) {
        for (CommandNode child : node.children()) {
            builder.then(convertChild(child));
        }
        if (!node.flags().isEmpty()) {
            RequiredArgumentBuilder<N, String> tunnel =
                RequiredArgumentBuilder.argument(FLAG_TUNNEL, StringArgumentType.greedyString());
            tunnel.suggests((context, suggestionsBuilder) -> {
                CommandSource source = sourceMapper.apply(context.getSource());
                CommandInput input = input(source, context.getInput(), context.getInput().length());
                for (dev.riege.buildmycommand.api.Suggestion suggestion : framework.suggestRich(input)) {
                    if (suggestion.value().startsWith("-")) {
                        suggestionsBuilder.suggest(suggestion.value());
                    }
                }
                return suggestionsBuilder.buildFuture();
            });
            attachExecutor(tunnel);
            builder.then(tunnel);
        }
    }

    private void attachExecutor(ArgumentBuilder<N, ?> builder) {
        builder.executes(context -> {
            CommandSource source = sourceMapper.apply(context.getSource());
            CommandResult result = framework.dispatch(input(source, context.getInput(), context.getInput().length()));
            return result.status() == CommandResult.Status.SUCCESS ? Command.SINGLE_SUCCESS : 0;
        });
    }

    private boolean canAccess(N nativeSource, CommandNode node) {
        return node.permission()
            .map(permission -> sourceMapper.apply(nativeSource).hasPermission(permission))
            .orElse(true);
    }

    private static ArgumentType<?> brigadierArgument(ArgumentSpec<?> argument) {
        Class<?> type = argument.type();
        if (type == int.class || type == Integer.class) {
            return IntegerArgumentType.integer();
        }
        if (type == long.class || type == Long.class) {
            return LongArgumentType.longArg();
        }
        if (type == double.class || type == Double.class) {
            return DoubleArgumentType.doubleArg();
        }
        if (type == boolean.class || type == Boolean.class) {
            return BoolArgumentType.bool();
        }
        if (argument.kind() == ArgumentSpec.Kind.GREEDY || argument.kind() == ArgumentSpec.Kind.OPTIONAL_GREEDY) {
            return StringArgumentType.greedyString();
        }
        return StringArgumentType.string();
    }

    private static String normalize(String input) {
        while (input.startsWith("/")) {
            input = input.substring(1);
        }
        return input;
    }

    private CommandInput input(CommandSource source, String rawInput, int cursor) {
        String prefix = rawInput.startsWith("/") ? "/" : "";
        return new CommandInput(source, rawInput, normalize(rawInput), cursor, prefix, platform);
    }
}
