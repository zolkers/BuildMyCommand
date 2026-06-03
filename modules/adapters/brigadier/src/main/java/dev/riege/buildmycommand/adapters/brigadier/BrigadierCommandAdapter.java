package dev.riege.buildmycommand.adapters.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.ArgumentSpec;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class BrigadierCommandAdapter<N> implements CommandAdapter<N, String, Integer> {
    private static final String FRAMEWORK_TUNNEL = "_bmc_input";
    private static final SimpleCommandExceptionType INVALID_INPUT =
        new SimpleCommandExceptionType(new LiteralMessage("Invalid command input"));

    private final CommandFramework framework;
    private final Function<N, CommandSource> sourceMapper;
    private final CommandPlatform platform;
    private final AdapterRuntime runtime;
    private final AdapterConfig config;

    private BrigadierCommandAdapter(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper,
        CommandPlatform platform,
        AdapterConfig config
    ) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.runtime = new AdapterRuntime(framework, platform);
        this.config = Objects.requireNonNull(config, "config");
    }

    public static <N> BrigadierCommandAdapter<N> create(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper
    ) {
        CommandPlatform platform = new CommandPlatform("brigadier", "Brigadier", false, true, true);
        return create(framework, sourceMapper, platform,
            new AdapterConfig("brigadier", "Brigadier", AdapterCapabilities.from(platform)));
    }

    public static <N> BrigadierCommandAdapter<N> create(
        CommandFramework framework,
        Function<N, CommandSource> sourceMapper,
        CommandPlatform platform,
        AdapterConfig config
    ) {
        return new BrigadierCommandAdapter<>(framework, sourceMapper, platform, config);
    }

    public List<LiteralCommandNode<N>> roots() {
        List<LiteralCommandNode<N>> roots = new ArrayList<>();
        for (BrigadierRoot<N> root : projectedRoots()) {
            roots.add(root.root());
            roots.addAll(root.aliasRoots());
        }
        return List.copyOf(roots);
    }

    public List<BrigadierRoot<N>> projectedRoots() {
        List<BrigadierRoot<N>> roots = new ArrayList<>();
        for (CommandNode root : framework.graph().roots()) {
            LiteralCommandNode<N> rootNode = convertRoot(root).build();
            List<LiteralCommandNode<N>> aliasRoots = root.aliases().stream()
                .map(alias -> LiteralArgumentBuilder.<N>literal(alias)
                    .executes(rootNode.getCommand())
                    .redirect(rootNode)
                    .build())
                .toList();
            roots.add(new BrigadierRoot<>(rootNode, root.aliases(), aliasRoots));
        }
        return List.copyOf(roots);
    }

    public List<String> rootLiterals() {
        return framework.graph().roots().stream().map(CommandNode::literal).toList();
    }

    public boolean caseInsensitiveLiterals() {
        return framework.caseInsensitiveLiterals();
    }

    public BrigadierRegistration<N> registration() {
        return new BrigadierRegistration<>(this);
    }

    private LiteralArgumentBuilder<N> convertRoot(CommandNode node) {
        LiteralArgumentBuilder<N> builder = LiteralArgumentBuilder.<N>literal(node.literal());
        attachExecutorIfRunnable(builder, node, -1);
        attachNodeContents(builder, node);
        return builder;
    }

    private LiteralCommandNode<N> convertChild(CommandNode node) {
        LiteralArgumentBuilder<N> builder = LiteralArgumentBuilder.<N>literal(node.literal());
        attachExecutorIfRunnable(builder, node, -1);
        attachNodeContents(builder, node);
        return builder.build();
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
            for (Suggestion suggestion : framework.suggestRich(input)) {
                suggestionsBuilder.suggest(suggestion.value(),
                    suggestion.tooltip().map(LiteralMessage::new).orElse(null));
            }
            return suggestionsBuilder.buildFuture();
        });
        attachExecutorIfRunnable(builder, node, index);
        if (index + 1 < node.arguments().size()) {
            builder.then(argumentBranch(node, index + 1));
        } else {
            attachTerminalContents(builder, node);
        }
        return builder;
    }

    private void attachTerminalContents(ArgumentBuilder<N, ?> builder, CommandNode node) {
        for (CommandNode child : node.children()) {
            LiteralCommandNode<N> childNode = convertChild(child);
            builder.then(childNode);
            for (String alias : child.aliases()) {
                builder.then(LiteralArgumentBuilder.<N>literal(alias)
                    .executes(childNode.getCommand())
                    .redirect(childNode)
                    .build());
            }
        }
        if (needsFrameworkTunnel(node)) {
            RequiredArgumentBuilder<N, String> tunnel =
                RequiredArgumentBuilder.argument(FRAMEWORK_TUNNEL, StringArgumentType.greedyString());
            attachFrameworkSuggestions(tunnel);
            tunnel.executes(context -> executeTunnel(context, node));
            builder.then(tunnel);
        }
    }

    private boolean needsFrameworkTunnel(CommandNode node) {
        return !node.flags().isEmpty() || (framework.caseInsensitiveLiterals() && !node.children().isEmpty());
    }

    private int executeTunnel(CommandContext<N> context, CommandNode node) throws CommandSyntaxException {
        if (shouldRejectTunnel(node, context.getArgument(FRAMEWORK_TUNNEL, String.class))) {
            throw INVALID_INPUT.create();
        }
        return execute(context.getSource(), context.getInput());
    }

    private boolean shouldRejectTunnel(CommandNode node, String tunnelInput) {
        if (node.children().isEmpty()) {
            return false;
        }
        String token = firstToken(tunnelInput);
        if (token == null || token.startsWith("-")) {
            return false;
        }
        boolean exactKnownChild = false;
        boolean caseInsensitiveKnownChild = false;
        for (CommandNode child : node.children()) {
            for (String label : childLabels(child)) {
                exactKnownChild |= label.equals(token);
                caseInsensitiveKnownChild |= framework.caseInsensitiveLiterals() && label.equalsIgnoreCase(token);
            }
        }
        if (exactKnownChild) {
            return true;
        }
        return !caseInsensitiveKnownChild;
    }

    private static List<String> childLabels(CommandNode child) {
        List<String> labels = new ArrayList<>();
        labels.add(child.literal());
        labels.addAll(child.aliases());
        return labels;
    }

    private static String firstToken(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int end = 0;
        while (end < trimmed.length() && !Character.isWhitespace(trimmed.charAt(end))) {
            end++;
        }
        return trimmed.substring(0, end);
    }

    private void attachFrameworkSuggestions(RequiredArgumentBuilder<N, String> builder) {
        builder.suggests((context, suggestionsBuilder) -> {
            CommandSource source = sourceMapper.apply(context.getSource());
            CommandInput input = input(source, context.getInput(), context.getInput().length());
            for (Suggestion suggestion : framework.suggestRich(input)) {
                suggestionsBuilder.suggest(suggestion.value(),
                    suggestion.tooltip().map(LiteralMessage::new).orElse(null));
            }
            return suggestionsBuilder.buildFuture();
        });
    }

    private void attachExecutor(ArgumentBuilder<N, ?> builder) {
        builder.executes(context -> {
            return execute(context.getSource(), context.getInput());
        });
    }

    private void attachExecutorIfRunnable(ArgumentBuilder<N, ?> builder, CommandNode node, int lastParsedArgumentIndex) {
        if (node.executor().isEmpty()) {
            return;
        }
        for (int index = lastParsedArgumentIndex + 1; index < node.arguments().size(); index++) {
            ArgumentSpec.Kind kind = node.arguments().get(index).kind();
            if (kind == ArgumentSpec.Kind.REQUIRED || kind == ArgumentSpec.Kind.GREEDY) {
                return;
            }
        }
        attachExecutor(builder);
    }

    private static ArgumentType<?> brigadierArgument(ArgumentSpec<?> argument) {
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

    @Override
    public AdapterRuntime runtime() {
        return runtime;
    }

    @Override
    public AdapterConfig config() {
        return config;
    }

    @Override
    public AdapterRenderer<Integer> renderer() {
        return result -> result.status() == CommandResult.Status.SUCCESS ? Command.SINGLE_SUCCESS : 0;
    }

    @Override
    public CommandSource mapSource(N nativeSource) {
        Objects.requireNonNull(nativeSource, "nativeSource");
        return Objects.requireNonNull(sourceMapper.apply(nativeSource), "mapped source");
    }

    @Override
    public CommandInput mapInput(N nativeSource, String nativeInput) {
        Objects.requireNonNull(nativeInput, "nativeInput");
        return input(mapSource(nativeSource), nativeInput, nativeInput.length());
    }

    private CommandInput input(CommandSource source, String rawInput, int cursor) {
        String prefix = rawInput.startsWith("/") ? "/" : "";
        return new CommandInput(source, rawInput, normalize(rawInput), cursor, prefix, platform);
    }
}
