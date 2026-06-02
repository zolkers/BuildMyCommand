package dev.riege.buildmycommand.adapters.terminal;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TerminalAdapter implements CommandAdapter<CommandSource, String, Void> {
    private final AdapterRuntime runtime;
    private final AdapterConfig config;
    private InputStream input;
    private PrintStream output;
    private BufferedReader reader;
    private boolean historyEnabled;
    private String exitCommand = "exit";
    private final List<String> history = new ArrayList<>();

    private TerminalAdapter(CommandFramework framework) {
        CommandPlatform platform = CommandPlatform.terminal();
        this.runtime = new AdapterRuntime(Objects.requireNonNull(framework, "framework"), platform);
        this.config = AdapterConfig.of(platform.id(), platform.displayName(), AdapterCapabilities.from(platform));
        this.input = System.in;
        this.output = System.out;
    }

    public static TerminalAdapter attach(CommandFramework framework) {
        return new TerminalAdapter(framework);
    }

    public TerminalAdapter input(InputStream input) {
        this.input = Objects.requireNonNull(input, "input");
        this.reader = null;
        return this;
    }

    public TerminalAdapter output(PrintStream output) {
        this.output = Objects.requireNonNull(output, "output");
        return this;
    }

    public TerminalAdapter historyEnabled(boolean historyEnabled) {
        this.historyEnabled = historyEnabled;
        return this;
    }

    public TerminalAdapter exitCommand(String exitCommand) {
        Objects.requireNonNull(exitCommand, "exitCommand");
        if (exitCommand.isBlank()) {
            throw new IllegalArgumentException("exitCommand must not be blank");
        }
        this.exitCommand = exitCommand;
        return this;
    }

    public List<String> history() {
        return List.copyOf(history);
    }

    public void runOnce() {
        runOnce(new TerminalCommandSource(output));
    }

    public void runOnce(CommandSource source) {
        Objects.requireNonNull(source, "source");
        String line = readLine();
        if (line != null) {
            executeLine(source, line);
        }
    }

    public void runLoop() {
        runLoop(new TerminalCommandSource(output));
    }

    public void runLoop(CommandSource source) {
        Objects.requireNonNull(source, "source");
        while (true) {
            String line = readLine();
            if (line == null || isExit(line)) {
                return;
            }
            executeLine(source, line);
        }
    }

    public List<String> complete(CommandSource source, String input, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return runtime.framework().suggest(source, input, cursor);
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
    public AdapterRenderer<Void> renderer() {
        return result -> {
            result.reply().ifPresent(output::println);
            return null;
        };
    }

    @Override
    public CommandSource mapSource(CommandSource nativeSource) {
        return Objects.requireNonNull(nativeSource, "nativeSource");
    }

    @Override
    public CommandInput mapInput(CommandSource nativeSource, String nativeInput) {
        Objects.requireNonNull(nativeInput, "nativeInput");
        return new CommandInput(mapSource(nativeSource), nativeInput, nativeInput.length(), "", runtime.platform());
    }

    private String readLine() {
        try {
            return reader().readLine();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read terminal input", exception);
        }
    }

    private void executeLine(CommandSource source, String line) {
        if (historyEnabled && !line.isBlank()) {
            history.add(line);
        }
        execute(source, line);
    }

    private boolean isExit(String line) {
        return exitCommand.equals(line.trim());
    }

    private BufferedReader reader() {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return reader;
    }

    private record TerminalCommandSource(PrintStream output) implements CommandSource {
        private TerminalCommandSource {
            Objects.requireNonNull(output, "output");
        }

        @Override
        public void reply(String message) {
            output.println(message);
        }
    }
}
