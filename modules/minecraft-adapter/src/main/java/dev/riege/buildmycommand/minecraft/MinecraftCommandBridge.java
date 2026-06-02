package dev.riege.buildmycommand.minecraft;

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

    public CommandResult dispatch(S source, String commandLine) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");
        return framework.dispatch(sourceMapper.map(source), stripLeadingSlash(commandLine));
    }

    public List<String> suggest(S source, String commandLine, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(commandLine, "commandLine");

        String normalized = stripLeadingSlash(commandLine);
        int normalizedCursor = commandLine.startsWith("/") ? Math.max(0, cursor - 1) : cursor;
        return framework.suggest(sourceMapper.map(source), normalized, normalizedCursor);
    }

    private static String stripLeadingSlash(String commandLine) {
        return commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
    }
}
