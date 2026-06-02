package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class MinecraftCommandBridge<S> {
    private static final CommandPlatform MINECRAFT_PLATFORM =
        new CommandPlatform("minecraft", "Minecraft", false, true, true);

    private final CommandFramework framework;
    private final MinecraftSourceMapper<S> sourceMapper;

    public MinecraftCommandBridge(CommandFramework framework, MinecraftSourceMapper<S> sourceMapper) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
    }

    public List<String> rootLiterals() {
        return framework.rootLiterals();
    }

    public List<String> rootLabels() {
        return framework.rootLabels();
    }

    public boolean caseInsensitiveLiterals() {
        return framework.caseInsensitiveLiterals();
    }

    public boolean caseInsensitiveOptions() {
        return framework.caseInsensitiveOptions();
    }

    public CommandResult dispatch(S source, String commandLine) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");
        return framework.dispatch(input(source, commandLine, commandLine.length()));
    }

    public CommandResult dispatch(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return framework.dispatch(input(source, invocation));
    }

    public List<String> suggest(S source, String commandLine, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");

        return framework.suggestRich(input(source, commandLine, cursor)).stream()
            .map(dev.riege.buildmycommand.api.Suggestion::value)
            .toList();
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(source, "source");
        return framework.suggestRich(input(source, invocation)).stream()
            .map(dev.riege.buildmycommand.api.Suggestion::value)
            .toList();
    }

    private CommandInput input(S source, String commandLine, int cursor) {
        String prefix = commandLine.startsWith("/") ? "/" : "";
        String normalized = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        return new CommandInput(sourceMapper.map(source), commandLine, normalized, cursor, prefix, MINECRAFT_PLATFORM);
    }

    private CommandInput input(S source, MinecraftInvocation invocation) {
        String prefix = invocation.rawInput().startsWith("/") ? "/" : "";
        int cursor = prefix.isEmpty() ? invocation.cursor() : invocation.cursor() + prefix.length();
        return new CommandInput(
            sourceMapper.map(source),
            invocation.rawInput(),
            invocation.normalizedInput(),
            cursor,
            prefix,
            MINECRAFT_PLATFORM
        );
    }
}
