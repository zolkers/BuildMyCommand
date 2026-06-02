package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
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
            .map(dev.riege.buildmycommand.api.Suggestion::value)
            .toList();
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return runtime.framework().suggestRich(input(source, invocation)).stream()
            .map(dev.riege.buildmycommand.api.Suggestion::value)
            .toList();
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
        return sourceMapper.map(nativeSource);
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
        String prefix = invocation.rawInput().startsWith("/") ? "/" : "";
        int cursor = prefix.isEmpty() ? invocation.cursor() : invocation.cursor() + prefix.length();
        return new CommandInput(
            mapSource(source),
            invocation.rawInput(),
            invocation.normalizedInput(),
            cursor,
            prefix,
            MINECRAFT_PLATFORM
        );
    }
}
