package dev.buildmycommand.annotation;

import dev.buildmycommand.api.CommandContext;
import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationCommandScannerTest {
    @Test
    void registersAnnotatedCommandWithStringArgumentAndBooleanFlag() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());

        CommandResult result = framework.dispatch(source(), "ban Steve --silent");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve:true"), result.reply());
    }

    @Test
    void registersAnnotatedCommandWithIntegerArgumentAndContext() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new NumberCommands());

        CommandResult result = framework.dispatch(source(), "level Alex 7");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("level Alex=7 via level Alex 7"), result.reply());
    }

    @Test
    void rejectsUnsupportedAnnotatedParameterAtRegistration() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new InvalidCommands()));

        assertEquals("unsupported annotated command parameter: reason", exception.getMessage());
    }

    @Test
    void registersMultipleAnnotatedCommandsInDeterministicCommandOrder() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new OrderedCommands());

        assertEquals("""
            command alpha
            command zeta""", framework.schema());
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }

    static final class ModerationCommands {
        @Command("ban")
        CommandResult ban(@Arg("target") String target, @Flag("silent") boolean silent) {
            return Results.success(target + ":" + silent);
        }
    }

    static final class NumberCommands {
        @Command("level")
        public CommandResult level(CommandContext context, @Arg("target") String target, @Arg("amount") Integer amount) {
            return Results.success("level " + target + "=" + amount + " via " + context.input());
        }
    }

    static final class InvalidCommands {
        @Command("bad")
        CommandResult bad(@Arg("reason") Double reason) {
            return Results.success(String.valueOf(reason));
        }
    }

    static final class OrderedCommands {
        @Command("zeta")
        CommandResult zeta() {
            return Results.success("zeta");
        }

        @Command("alpha")
        CommandResult alpha() {
            return Results.success("alpha");
        }
    }
}
