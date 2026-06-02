package dev.riege.buildmycommand.adapters.minecraft.common;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class MinecraftCommandBridge<S> {
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
        return framework.dispatch(sourceMapper.map(source), stripLeadingSlash(commandLine));
    }

    public CommandResult dispatch(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return framework.dispatch(sourceMapper.map(Objects.requireNonNull(source, "source")), invocation.normalizedInput());
    }

    public List<String> suggest(S source, String commandLine, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");

        String normalized = stripLeadingSlash(commandLine);
        int normalizedCursor = commandLine.startsWith("/") ? Math.max(0, cursor - 1) : cursor;
        return framework.suggest(sourceMapper.map(source), normalized, normalizedCursor);
    }

    public List<String> suggest(S source, MinecraftInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return framework.suggest(
            sourceMapper.map(Objects.requireNonNull(source, "source")),
            invocation.normalizedInput(),
            invocation.cursor()
        );
    }

    private static String stripLeadingSlash(String commandLine) {
        return commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
    }
}
