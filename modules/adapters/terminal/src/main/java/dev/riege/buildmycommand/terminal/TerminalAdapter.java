package dev.riege.buildmycommand.terminal;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

@Deprecated(forRemoval = false, since = "0.1.0")
public final class TerminalAdapter implements CommandAdapter<CommandSource, String, Void> {
    private final dev.riege.buildmycommand.adapters.terminal.TerminalAdapter delegate;

    private TerminalAdapter(dev.riege.buildmycommand.adapters.terminal.TerminalAdapter delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static TerminalAdapter attach(CommandFramework framework) {
        return new TerminalAdapter(dev.riege.buildmycommand.adapters.terminal.TerminalAdapter.attach(framework));
    }

    public TerminalAdapter input(InputStream input) {
        delegate.input(input);
        return this;
    }

    public TerminalAdapter output(PrintStream output) {
        delegate.output(output);
        return this;
    }

    public TerminalAdapter historyEnabled(boolean historyEnabled) {
        delegate.historyEnabled(historyEnabled);
        return this;
    }

    public TerminalAdapter exitCommand(String exitCommand) {
        delegate.exitCommand(exitCommand);
        return this;
    }

    public List<String> history() {
        return delegate.history();
    }

    public void runOnce() {
        delegate.runOnce();
    }

    public void runOnce(CommandSource source) {
        delegate.runOnce(source);
    }

    public void runLoop() {
        delegate.runLoop();
    }

    public void runLoop(CommandSource source) {
        delegate.runLoop(source);
    }

    public List<String> complete(CommandSource source, String input, int cursor) {
        return delegate.complete(source, input, cursor);
    }

    @Override
    public AdapterRuntime runtime() {
        return delegate.runtime();
    }

    @Override
    public AdapterConfig config() {
        return delegate.config();
    }

    @Override
    public AdapterRenderer<Void> renderer() {
        return delegate.renderer();
    }

    @Override
    public CommandSource mapSource(CommandSource nativeSource) {
        return delegate.mapSource(nativeSource);
    }

    @Override
    public CommandInput mapInput(CommandSource nativeSource, String nativeInput) {
        return delegate.mapInput(nativeSource, nativeInput);
    }

    @Override
    public AdapterCapabilities capabilities() {
        return delegate.capabilities();
    }
}
