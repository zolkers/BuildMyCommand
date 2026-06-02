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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(java.util.List.of(), adapter.history());
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

        assertEquals(java.util.List.of("quiet"), adapter.history());
    }

    @Test
    void completeDelegatesToFrameworkSuggestions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.silent()));
        framework.registry().command("pong", command -> command.executes(ctx -> Results.silent()));
        TerminalAdapter adapter = TerminalAdapter.attach(framework);

        assertEquals(java.util.List.of("ping"), adapter.complete(source(), "pi", 2));
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
            java.util.List.of("ping"),
            java.util.List.of("ping", "p")
        ), sdkAdapter.registrationLabels());
        assertEquals(line("terminal"), text(captured));
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
