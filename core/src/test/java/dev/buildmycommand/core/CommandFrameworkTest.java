package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandFrameworkTest {
    @Test
    void dispatchesRegisteredLiteralCommand() {
        CommandFramework framework = CommandFramework.create();
        CommandSource source = new CommandSource() {
        };

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        CommandResult result = framework.dispatch(source, "ping");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Pong"), result.reply());
    }

    @Test
    void returnsFailureForUnknownCommand() {
        CommandFramework framework = CommandFramework.create();

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "missing");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unknown command: missing"), result.reply());
    }

    @Test
    void rejectsNullDispatchInputs() {
        CommandFramework framework = CommandFramework.create();
        CommandSource source = new CommandSource() {
        };

        assertThrows(NullPointerException.class, () -> framework.dispatch(null, "ping"));
        assertThrows(NullPointerException.class, () -> framework.dispatch(source, null));
    }

    @Test
    void rejectsDuplicateCommandLiteral() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Again"))));
    }

    @Test
    void rejectsNullCommandResult() {
        CommandFramework framework = CommandFramework.create();
        CommandSource source = new CommandSource() {
        };

        framework.registry().command("broken", command -> command.executes(ctx -> null));

        assertThrows(NullPointerException.class, () -> framework.dispatch(source, "broken"));
    }

    @Test
    void rejectsNullResultReplies() {
        assertThrows(NullPointerException.class, () -> Results.success(null));
        assertThrows(NullPointerException.class, () -> Results.failure(null));
    }

    @Test
    void parsesQuotedStringArgumentAsOneValue() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say \"hello world\"");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("hello world"), result.reply());
    }

    @Test
    void failsWhenRequiredArgumentIsMissing() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing required argument: message"), result.reply());
    }

    @Test
    void parsesOptionalArgumentWhenPresent() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("greet", command -> command
            .optionalArgument("name", String.class)
            .executes(ctx -> Results.success(ctx.optionalArg("name", String.class).orElse("stranger"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "greet Ada");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Ada"), result.reply());
    }

    @Test
    void usesFallbackForMissingOptionalArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("greet", command -> command
            .optionalArgument("name", String.class)
            .executes(ctx -> Results.success(ctx.argOr("name", "stranger"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "greet");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("stranger"), result.reply());
    }

    @Test
    void parsesGreedyArgumentFromRemainingInput() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("announce", command -> command
            .greedyArgument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "announce hello brave new world");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("hello brave new world"), result.reply());
    }

    @Test
    void failsInsteadOfThrowingForInvalidIntegerArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("repeat", command -> command
            .argument("count", Integer.class)
            .executes(ctx -> Results.success(String.valueOf(ctx.arg("count", Integer.class)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "repeat many");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Invalid integer for argument count: many"), result.reply());
    }

    @Test
    void rejectsRequiredArgumentAfterOptionalArgument() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalStateException.class, () -> framework.registry().command("bad", command -> command
            .optionalArgument("maybe", String.class)
            .argument("required", String.class)
            .executes(ctx -> Results.silent())));
    }

    @Test
    void rejectsArgumentAfterGreedyArgument() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalStateException.class, () -> framework.registry().command("bad", command -> command
            .greedyArgument("message", String.class)
            .argument("tail", String.class)
            .executes(ctx -> Results.silent())));
    }

    @Test
    void supportsPrimitiveIntegerArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("repeat", command -> command
            .argument("count", int.class)
            .executes(ctx -> Results.success(String.valueOf(ctx.arg("count", int.class)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "repeat 3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("3"), result.reply());
    }

    @Test
    void rejectsDuplicateArgumentNames() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalStateException.class, () -> framework.registry().command("bad", command -> command
            .argument("value", String.class)
            .argument("value", String.class)
            .executes(ctx -> Results.silent())));
    }

    @Test
    void failsForUnclosedQuote() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say \"hello");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unclosed quote"), result.reply());
    }

    @Test
    void failsForUnexpectedExtraArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say hello extra");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unexpected argument: extra"), result.reply());
    }

    @Test
    void dispatchesNestedSubcommandArguments() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .subcommand("rank", rank -> rank
                .subcommand("set", set -> set
                    .argument("target", String.class)
                    .argument("rank", String.class)
                    .executes(ctx -> Results.success(ctx.arg("target", String.class) + "=" + ctx.arg("rank", String.class))))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank set Victor admin");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Victor=admin"), result.reply());
    }

    @Test
    void matchesSubcommandLiteralBeforeParsingArguments() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .argument("target", String.class)
            .executes(ctx -> Results.success("profile " + ctx.arg("target", String.class)))
            .subcommand("rank", rank -> rank
                .argument("target", String.class)
                .executes(ctx -> Results.success("rank " + ctx.arg("target", String.class)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank Victor");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("rank Victor"), result.reply());
    }

    @Test
    void dispatchesTopLevelCommandAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command
            .alias("p")
            .executes(ctx -> Results.success("Pong")));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "p");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Pong"), result.reply());
    }

    @Test
    void dispatchesSubcommandAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .subcommand("rank", rank -> rank
                .aliases("r", "roles")
                .executes(ctx -> Results.success("rank"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user roles");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("rank"), result.reply());
    }

    @Test
    void rejectsTopLevelAliasConflict() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("pong", command -> command
            .alias("ping")
            .executes(ctx -> Results.success("Again"))));
    }

    @Test
    void rejectsSiblingSubcommandAliasConflict() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("user", user -> user
            .subcommand("rank", rank -> rank.executes(ctx -> Results.success("rank")))
            .subcommand("role", role -> role
                .alias("rank")
                .executes(ctx -> Results.success("role")))));
    }

    @Test
    void rejectsDuplicateAliasesOnSameBuilder() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("ping", command -> command
            .aliases("p", "p")
            .executes(ctx -> Results.success("Pong"))));
    }
}
