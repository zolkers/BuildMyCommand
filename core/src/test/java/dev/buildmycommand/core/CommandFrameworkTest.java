package dev.buildmycommand.core;

import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void parsesBooleanFlagAfterPositionalArguments() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ban", command -> command
            .argument("target", String.class)
            .flag("silent")
            .executes(ctx -> Results.success(ctx.arg("target", String.class) + ":" + ctx.flag("silent"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ban Steve --silent");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve:true"), result.reply());
    }

    @Test
    void parsesValuedOptionAfterPositionalArguments() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("give", command -> command
            .argument("target", String.class)
            .argument("item", String.class)
            .option("amount", Integer.class)
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class) + " gets "
                    + ctx.option("amount", Integer.class).orElse(1)
                    + " " + ctx.arg("item", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "give Steve diamond --amount 64");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve gets 64 diamond"), result.reply());
    }

    @Test
    void parsesShortFlagAndOptionAliases() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("give", command -> command
            .argument("target", String.class)
            .argument("item", String.class)
            .flag("silent", "s")
            .option("amount", Integer.class, "a")
            .executes(ctx -> Results.success(
                ctx.flag("silent") + ":" + ctx.option("amount", Integer.class).orElse(1))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "give Steve diamond -s -a 64");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("true:64"), result.reply());
    }

    @Test
    void failsForUnknownFlagOrOption() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ban", command -> command
            .argument("target", String.class)
            .flag("silent")
            .executes(ctx -> Results.silent()));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ban Steve --force");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unknown flag or option: --force"), result.reply());
    }

    @Test
    void failsWhenOptionValueIsMissing() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("give", command -> command
            .argument("target", String.class)
            .option("amount", Integer.class)
            .executes(ctx -> Results.silent()));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "give Steve --amount");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing value for option: amount"), result.reply());
    }

    @Test
    void parsesNegativeIntegerPositionalArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("repeat", command -> command
            .argument("count", Integer.class)
            .executes(ctx -> Results.success(String.valueOf(ctx.arg("count", Integer.class)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "repeat -3");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("-3"), result.reply());
    }

    @Test
    void parsesNegativeIntegerOptionValue() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("move", command -> command
            .option("offset", Integer.class)
            .executes(ctx -> Results.success(String.valueOf(ctx.option("offset", Integer.class).orElse(0)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "move --offset -5");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("-5"), result.reply());
    }

    @Test
    void rejectsOptionNameConflictingWithArgumentName() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalStateException.class, () -> framework.registry().command("ban", command -> command
            .argument("target", String.class)
            .flag("target")
            .executes(ctx -> Results.silent())));
    }

    @Test
    void rejectsArgumentNameConflictingWithExistingOptionName() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalStateException.class, () -> framework.registry().command("ban", command -> command
            .flag("target")
            .argument("target", String.class)
            .executes(ctx -> Results.silent())));
    }

    @Test
    void suggestsRootCommandsByPrefix() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.silent()));
        framework.registry().command("ban", command -> command.executes(ctx -> Results.silent()));

        List<String> suggestions = framework.suggest(new CommandSource() {
        }, "p", 1);

        assertEquals(List.of("ping"), suggestions);
    }

    @Test
    void suggestsFlagsForMatchedCommand() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ban", command -> command
            .argument("target", String.class)
            .flag("silent", "s")
            .option("reason", String.class, "r")
            .executes(ctx -> Results.silent()));

        List<String> suggestions = framework.suggest(new CommandSource() {
        }, "ban Steve --", 12);

        assertEquals(List.of("--silent", "--reason"), suggestions);
    }

    @Test
    void routeDslDispatchesGreedyOptionalArgumentAndFlagAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban <target:String> [reason:String...] [--silent|-s]")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + ":" + ctx.argOr("reason", "")
                    + ":" + ctx.flag("silent")));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ban Steve griefing in spawn -s");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve:griefing in spawn:true"), result.reply());
    }

    @Test
    void routeDslDispatchesNestedLiteralsRequiredArgumentsAndValuedOptionAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank set <target:String> <count:int> [--amount:Integer|-a]")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + ":" + ctx.arg("count", int.class)
                    + ":" + ctx.option("amount", Integer.class).orElse(0)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank set Alex 2 -a 5");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Alex:2:5"), result.reply());
    }

    @Test
    void routeDslRejectsInvalidPatternAtRegistration() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban <target:Double>").executes(ctx -> Results.silent()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban [--silent|-silent]").executes(ctx -> Results.silent()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban [reason:String...] <tail:String>").executes(ctx -> Results.silent()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban [reason:String] <target:String>").executes(ctx -> Results.silent()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban <target:String> [--target]").executes(ctx -> Results.silent()));
    }

    @Test
    void routeDslMergesRoutesSharingTheSameRoot() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank set <target:String>")
            .executes(ctx -> Results.success("set " + ctx.arg("target", String.class)));
        framework.registry()
            .route("user rank remove <target:String>")
            .executes(ctx -> Results.success("remove " + ctx.arg("target", String.class)));

        CommandResult set = framework.dispatch(new CommandSource() {
        }, "user rank set Alex");
        CommandResult remove = framework.dispatch(new CommandSource() {
        }, "user rank remove Alex");

        assertEquals(CommandResult.Status.SUCCESS, set.status());
        assertEquals(Optional.of("set Alex"), set.reply());
        assertEquals(CommandResult.Status.SUCCESS, remove.status());
        assertEquals(Optional.of("remove Alex"), remove.reply());
    }

    @Test
    void routeDslDispatchesLiteralAfterParentArgument() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user <target:String> rank set <rank:String>")
            .executes(ctx -> Results.success(ctx.arg("target", String.class) + "=" + ctx.arg("rank", String.class)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user Alex rank set admin");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Alex=admin"), result.reply());
    }

    @Test
    void routeDslRejectsNonStringGreedyArguments() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("sum <values:Integer...>").executes(ctx -> Results.silent()));
    }

    @Test
    void routeDslMergeKeepsExistingParentExecutor() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user")
            .executes(ctx -> Results.success("user root"));
        framework.registry()
            .route("user rank")
            .executes(ctx -> Results.success("rank root"));

        CommandResult root = framework.dispatch(new CommandSource() {
        }, "user");
        CommandResult child = framework.dispatch(new CommandSource() {
        }, "user rank");

        assertEquals(CommandResult.Status.SUCCESS, root.status());
        assertEquals(Optional.of("user root"), root.reply());
        assertEquals(CommandResult.Status.SUCCESS, child.status());
        assertEquals(Optional.of("rank root"), child.reply());
    }

    @Test
    void routeDslRejectsOptionsBeforeLaterLiterals() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("tool [--verbose] run").executes(ctx -> Results.silent()));
    }

    @Test
    void routeDslRejectsDuplicateExactRouteRegistration() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank")
            .executes(ctx -> Results.success("rank"));

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("user rank").executes(ctx -> Results.success("again")));
    }

    @Test
    void returnsHelpForCommandUsage() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban <target:String> [reason:String...] [--silent|-s]")
            .executes(ctx -> Results.silent());

        assertEquals("Usage: ban <target:String> [reason:String...] [--silent|-s]", framework.help("ban"));
    }

    @Test
    void returnsHelpForNestedCommandUsage() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank set <target:String>")
            .executes(ctx -> Results.silent());

        assertEquals("Usage: user rank set <target:String>", framework.help("user rank set"));
    }

    @Test
    void helpIncludesArgumentsDeclaredBeforeSubcommandLiterals() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user <target:String> rank set <rank:String>")
            .executes(ctx -> Results.silent());

        assertEquals("Usage: user <target:String> rank set <rank:String>", framework.help("user rank set"));
    }

    @Test
    void returnsUsefulHelpForUnknownCommandPath() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank set <target:String>")
            .executes(ctx -> Results.silent());

        assertEquals("Unknown command: user missing", framework.help("user missing"));
    }

    @Test
    void exportsDeterministicSchemaFromCommandTree() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban <target:String> [reason:String...] [--silent|-s]")
            .executes(ctx -> Results.silent());
        framework.registry()
            .route("user rank set <target:String>")
            .executes(ctx -> Results.silent());

        String expected = """
            command ban
              argument target:String required
              argument reason:String optional-greedy
              option silent:Boolean flag alias=s
            command user
              child rank
            command user rank
              child set
            command user rank set
              argument target:String required""";

        assertEquals(expected, framework.schema());
        assertEquals(expected, framework.schema());
    }
}
