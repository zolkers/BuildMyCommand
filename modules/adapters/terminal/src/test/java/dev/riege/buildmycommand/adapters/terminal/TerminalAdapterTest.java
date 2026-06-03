package dev.riege.buildmycommand.adapters.terminal;

import dev.riege.buildmycommand.adapters.AdapterRegistrationLabels;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerminalAdapterTest {
    @Test
    void terminalAdapterLivesInGenericAdaptersModuleAndUsesSdk() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command
            .alias("p")
            .executes(ctx -> Results.success(ctx.commandInput().platform().id())));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework).output(output(captured));

        CommandAdapter<CommandSource, String, Void> sdkAdapter = adapter;
        CommandInput input = sdkAdapter.mapInput(source(), "ping");
        CommandResult result = sdkAdapter.dispatch(source(), "ping");
        sdkAdapter.render(result);

        assertEquals("terminal", input.platform().id());
        assertEquals("ping", input.normalizedInput());
        assertEquals(new AdapterRegistrationLabels(
            List.of("ping"),
            List.of("ping", "p")
        ), sdkAdapter.registrationLabels());
        assertEquals(line("terminal"), text(captured));
    }

    @Test
    void runLoopSupportsHistoryCompletionAndExitCommand() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("ping\nquit\n"))
            .output(output(captured))
            .historyEnabled(true)
            .exitCommand("quit");

        adapter.runLoop(source());

        assertEquals(line("Pong"), text(captured));
        assertEquals(List.of("ping"), adapter.history());
        assertEquals(List.of("ping"), adapter.complete(source(), "pi", 2));
    }

    @Test
    void handlesEofBlankHistoryAndInvalidConfiguration() {
        CommandFramework framework = CommandFramework.create();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("\n"))
            .output(output(captured))
            .historyEnabled(true);

        adapter.runOnce(source());
        adapter.input(input(""));
        adapter.runOnce(source());

        assertEquals(line("Unknown command: "), text(captured));
        assertEquals(List.of(), adapter.history());
        assertThrows(NullPointerException.class, () -> TerminalAdapter.attach(null));
        assertThrows(NullPointerException.class, () -> adapter.input(null));
        assertThrows(NullPointerException.class, () -> adapter.output(null));
        assertThrows(NullPointerException.class, () -> adapter.exitCommand(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.exitCommand(" "));
        assertThrows(NullPointerException.class, () -> adapter.runOnce(null));
        assertThrows(NullPointerException.class, () -> adapter.runLoop(null));
        assertThrows(NullPointerException.class, () -> adapter.complete(null, "x", 1));
        assertThrows(NullPointerException.class, () -> adapter.complete(source(), null, 1));
        assertThrows(NullPointerException.class, () -> adapter.mapSource(null));
        assertThrows(NullPointerException.class, () -> adapter.mapInput(source(), null));
    }

    @Test
    void runLoopStopsOnEofAndWrapsInputFailures() {
        CommandFramework framework = CommandFramework.create();
        TerminalAdapter eof = TerminalAdapter.attach(framework).input(input(""));
        TerminalAdapter broken = TerminalAdapter.attach(framework).input(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        });

        eof.runLoop(source());
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> broken.runOnce(source()));

        assertEquals("Failed to read terminal input", exception.getMessage());
    }

    private static ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static PrintStream output(ByteArrayOutputStream captured) {
        return new PrintStream(captured, true, StandardCharsets.UTF_8);
    }

    private static String text(ByteArrayOutputStream captured) {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static String line(String value) {
        return value + System.lineSeparator();
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
