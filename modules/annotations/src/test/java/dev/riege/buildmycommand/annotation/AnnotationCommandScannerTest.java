package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
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

    @Test
    void registersAnnotatedCommandMetadata() {
        CommandFramework framework = CommandFramework.create();

        AnnotationCommandScanner.register(framework.registry(), new MetadataCommands());

        assertEquals("""
            Usage: reload
            Description: Reload configuration""", framework.help("reload"));
        assertEquals("""
            command reload
              description Reload configuration
              permission admin.reload""", framework.schema());

        CommandResult result = framework.dispatch(source(), "reload");
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("reloaded"), result.reply());
    }

    @Test
    void rejectsBlankAnnotatedCommandMetadata() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException description = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new BlankDescriptionCommands()));
        IllegalArgumentException permission = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandScanner.register(framework.registry(), new BlankPermissionCommands()));

        assertEquals("description must not be blank", description.getMessage());
        assertEquals("permission must not be blank", permission.getMessage());
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

    static final class MetadataCommands {
        @Command("reload")
        @Description("Reload configuration")
        @Permission("admin.reload")
        CommandResult reload() {
            return Results.success("reloaded");
        }
    }

    static final class BlankDescriptionCommands {
        @Command("bad-description")
        @Description(" ")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class BlankPermissionCommands {
        @Command("bad-permission")
        @Permission(" ")
        CommandResult bad() {
            return Results.silent();
        }
    }
}
