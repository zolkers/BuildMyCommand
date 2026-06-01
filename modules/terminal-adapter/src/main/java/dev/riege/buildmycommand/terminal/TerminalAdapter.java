package dev.riege.buildmycommand.terminal;

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

public final class TerminalAdapter {
    private final CommandFramework framework;
    private InputStream input;
    private PrintStream output;
    private BufferedReader reader;

    private TerminalAdapter(CommandFramework framework) {
        this.framework = Objects.requireNonNull(framework, "framework");
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

        CommandResult result = framework.dispatch(source, line);
        result.reply().ifPresent(output::println);
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
