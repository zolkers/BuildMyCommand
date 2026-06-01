package dev.buildmycommand.examples;

import dev.buildmycommand.terminal.TerminalAdapter;

import java.io.InputStream;
import java.io.PrintStream;

public final class TerminalExample {
    private TerminalExample() {
    }

    public static TerminalAdapter attach(InputStream input, PrintStream output) {
        return TerminalAdapter.attach(BuilderExample.create())
            .input(input)
            .output(output);
    }
}
