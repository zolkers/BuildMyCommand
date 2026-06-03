package dev.riege.buildmycommand.terminal;

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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerminalAdapterTest {
    @Test
    void runsOneInputLineAndPrintsSuccessReply() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        TerminalAdapter.attach(framework)
            .input(input("ping\n"))
            .output(output(captured))
            .runOnce(source());

        assertEquals(line("Pong"), text(captured));
    }

    @Test
    void printsFailureReply() {
        CommandFramework framework = CommandFramework.create();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        TerminalAdapter.attach(framework)
            .input(input("missing\n"))
            .output(output(captured))
            .runOnce(source());

        assertEquals(line("Unknown command: missing"), text(captured));
    }

    @Test
    void printsNothingForSilentResult() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("quiet", command -> command.executes(ctx -> Results.silent()));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        TerminalAdapter.attach(framework)
            .input(input("quiet\n"))
            .output(output(captured))
            .runOnce(source());

        assertEquals("", text(captured));
    }

    @Test
    void repeatedRunOnceCallsReadSubsequentInputLines() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("ping\nping\n"))
            .output(output(captured));

        adapter.runOnce(source());
        adapter.runOnce(source());

        assertEquals(line("Pong") + line("Pong"), text(captured));
    }

    @Test
    void runLoopReadsUntilExitCommandAndHistoryIsDisabledByDefault() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("ping\nping\nexit\nping\n"))
            .output(output(captured));

        adapter.runLoop(source());

        assertEquals(line("Pong") + line("Pong"), text(captured));
        assertEquals(List.of(), adapter.history());
    }

    @Test
    void runLoopCanRecordHistoryWhenEnabled() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("quiet", command -> command.executes(ctx -> Results.silent()));
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("quiet\nquit\n"))
            .historyEnabled(true)
            .exitCommand("quit");

        adapter.runLoop(source());

        assertEquals(List.of("quiet"), adapter.history());
    }

    @Test
    void completeDelegatesToFrameworkSuggestions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.silent()));
        framework.registry().command("pong", command -> command.executes(ctx -> Results.silent()));
        TerminalAdapter adapter = TerminalAdapter.attach(framework);

        assertEquals(List.of("ping"), adapter.complete(source(), "pi", 2));
    }

    @Test
    void noArgRunOnceUsesTerminalCommandSource() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("reply", command -> command.executes(ctx -> {
            ctx.source().reply("from source");
            return Results.silent();
        }));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        TerminalAdapter.attach(framework)
            .input(input("reply\n"))
            .output(output(captured))
            .runOnce();

        assertEquals(line("from source"), text(captured));
    }

    @Test
    void exposesGenericAdapterSdkWithoutChangingTerminalApi() {
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
    void legacyAdapterDelegatesNoArgLoopCapabilitiesAndValidation() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("reply", command -> command.executes(ctx -> {
            ctx.source().reply("from loop");
            return Results.silent();
        }));
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        TerminalAdapter adapter = TerminalAdapter.attach(framework)
            .input(input("reply\nexit\n"))
            .output(output(captured));
        CommandSource source = source();

        adapter.runLoop();

        assertEquals(line("from loop"), text(captured));
        assertEquals(adapter.config().capabilities(), adapter.capabilities());
        assertEquals("terminal", adapter.runtime().platform().id());
        assertEquals(source, adapter.mapSource(source));
        assertEquals("ping", adapter.mapInput(source, "ping").normalizedInput());
        assertThrows(NullPointerException.class, () -> TerminalAdapter.attach(null));
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
