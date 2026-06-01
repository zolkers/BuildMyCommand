package dev.buildmycommand.terminal;

import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(text(captured).contains("Pong"));
    }

    @Test
    void printsFailureReply() {
        CommandFramework framework = CommandFramework.create();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        TerminalAdapter.attach(framework)
            .input(input("missing\n"))
            .output(output(captured))
            .runOnce(source());

        assertTrue(text(captured).contains("Unknown command: missing"));
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

    private static ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static PrintStream output(ByteArrayOutputStream captured) {
        return new PrintStream(captured, true, StandardCharsets.UTF_8);
    }

    private static String text(ByteArrayOutputStream captured) {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
