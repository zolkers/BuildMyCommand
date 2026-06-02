package dev.riege.buildmycommand.terminal;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TerminalAdapter implements CommandAdapter<CommandSource, String, Void> {
    private final AdapterRuntime runtime;
    private final AdapterConfig config;
    private InputStream input;
    private PrintStream output;
    private BufferedReader reader;

    private TerminalAdapter(CommandFramework framework) {
        CommandPlatform platform = CommandPlatform.terminal();
        this.runtime = new AdapterRuntime(Objects.requireNonNull(framework, "framework"), platform);
        this.config = new AdapterConfig(
            platform.id(),
            platform.displayName(),
            AdapterCapabilities.from(platform)
        );
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

    public void runOnce() {
        runOnce(new TerminalCommandSource(output));
    }

    public void runOnce(CommandSource source) {
        Objects.requireNonNull(source, "source");

        String line = readLine();
        if (line == null) {
            return;
        }

        execute(source, line);
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
        return new CommandInput(
            mapSource(nativeSource),
            nativeInput,
            nativeInput.length(),
            "",
            runtime.platform()
        );
    }

    private String readLine() {
        try {
            return reader().readLine();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read terminal input", exception);
        }
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
