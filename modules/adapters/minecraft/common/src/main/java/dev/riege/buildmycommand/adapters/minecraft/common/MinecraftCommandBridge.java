package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MinecraftCommandBridge<S> implements CommandAdapter<S, MinecraftInvocation, CommandResult> {
    private static final CommandPlatform MINECRAFT_PLATFORM =
        new CommandPlatform("minecraft", "Minecraft", false, true, true);

    private final AdapterRuntime runtime;
    private final AdapterConfig config;
    private final MinecraftSourceMapper<S> sourceMapper;

    public MinecraftCommandBridge(CommandFramework framework, MinecraftSourceMapper<S> sourceMapper) {
        this.runtime = new AdapterRuntime(Objects.requireNonNull(framework, "framework"), MINECRAFT_PLATFORM);
        this.config = new AdapterConfig(
            "minecraft-command",
            "Minecraft Command",
            AdapterCapabilities.from(MINECRAFT_PLATFORM)
        );
        this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
    }

    public List<String> rootLiterals() {
        return runtime.framework().rootLiterals();
    }

    public List<String> rootLabels() {
        return runtime.framework().rootLabels();
    }

    public boolean caseInsensitiveLiterals() {
        return runtime.framework().caseInsensitiveLiterals();
    }

    public boolean caseInsensitiveOptions() {
        return runtime.framework().caseInsensitiveOptions();
    }

    public boolean canUseRootLabel(S source, String label) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(label, "label");
        CommandSource mapped = mapSource(source);
        for (CommandNode root : runtime.framework().graph().roots()) {
            if (matchesLabel(root, label)) {
                return canAccessNode(mapped, root);
            }
        }
        return true;
    }

    public CommandResult dispatch(S source, String commandLine) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");
        return runtime.dispatch(input(source, commandLine, commandLine.length()));
    }

    public CommandResult dispatch(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return CommandAdapter.super.dispatch(source, invocation);
    }

    public List<String> suggest(S source, String commandLine, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");

        return runtime.framework().suggestRich(input(source, commandLine, cursor)).stream()
            .map(Suggestion::value)
            .toList();
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return suggest(source, invocation, invocation.cursor());
    }

    @Override
    public List<String> suggest(S source, MinecraftInvocation invocation, int cursor) {
        return suggestRich(source, invocation, cursor).stream()
            .map(Suggestion::value)
            .toList();
    }

    @Override
    public List<Suggestion> suggestRich(S source, MinecraftInvocation invocation, int cursor) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return runtime.framework().suggestRich(input(source, invocation, cursor));
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
    public AdapterRenderer<CommandResult> renderer() {
        return AdapterRenderer.identity();
    }

    @Override
    public CommandSource mapSource(S nativeSource) {
        Objects.requireNonNull(nativeSource, "nativeSource");
        return Objects.requireNonNull(sourceMapper.map(nativeSource), "mapped source");
    }

    @Override
    public CommandInput mapInput(S nativeSource, MinecraftInvocation nativeInput) {
        Objects.requireNonNull(nativeInput, "nativeInput");
        Objects.requireNonNull(nativeSource, "nativeSource");
        return input(nativeSource, nativeInput);
    }

    private CommandInput input(S source, String commandLine, int cursor) {
        String prefix = commandLine.startsWith("/") ? "/" : "";
        String normalized = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        return new CommandInput(mapSource(source), commandLine, normalized, cursor, prefix, MINECRAFT_PLATFORM);
    }

    private CommandInput input(S source, MinecraftInvocation invocation) {
        return input(source, invocation, invocation.cursor());
    }

    private CommandInput input(S source, MinecraftInvocation invocation, int normalizedCursor) {
        String prefix = invocation.rawInput().startsWith("/") ? "/" : "";
        int cursor = prefix.isEmpty() ? normalizedCursor : normalizedCursor + prefix.length();
        return new CommandInput(
            mapSource(source),
            invocation.rawInput(),
            invocation.normalizedInput(),
            cursor,
            prefix,
            MINECRAFT_PLATFORM
        );
    }

    private boolean matchesLabel(CommandNode root, String label) {
        if (matches(root.literal(), label)) {
            return true;
        }
        for (String alias : root.aliases()) {
            if (matches(alias, label)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String expected, String actual) {
        if (caseInsensitiveLiterals()) {
            return expected.toLowerCase(Locale.ROOT).equals(actual.toLowerCase(Locale.ROOT));
        }
        return expected.equals(actual);
    }

    private static boolean canAccessNode(CommandSource source, CommandNode node) {
        if (node.permission().isPresent() && !source.hasPermission(node.permission().get())) {
            return false;
        }
        if (node.executor().isPresent()) {
            return true;
        }
        for (CommandNode child : node.children()) {
            if (canAccessNode(source, child)) {
                return true;
            }
        }
        return node.children().isEmpty();
    }
}
