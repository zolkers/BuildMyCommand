package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.api.Arguments;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Flags;
import dev.riege.buildmycommand.api.Results;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    void dispatchesUniversalCommandInputWithPrefixAndPlatform() {
        CommandFramework framework = CommandFramework.create();
        CommandPlatform platform = new CommandPlatform("test", "Test", false, true, true);
        CommandSource source = new CommandSource() {
        };

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        CommandResult result = framework.dispatch(new CommandInput(source, "/ping", 5, "/", platform));

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Pong"), result.reply());
    }

    @Test
    void returnsRichSuggestionsWithReplacementRanges() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        List<Suggestion> suggestions = framework.suggestRich(
            new CommandInput(new CommandSource() {
            }, "p", 1, "", CommandPlatform.test()));

        assertEquals(List.of(new Suggestion("ping", Optional.empty(), 0, 1, SuggestionType.COMMAND, 0)), suggestions);
    }

    @Test
    void exposesPublicCommandGraphSnapshotForAdapters() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank set <target:String> [--silent|-s]")
            .description("Set rank")
            .executes(ctx -> Results.silent());

        assertEquals("user", framework.graph().roots().get(0).literal());
        assertEquals("rank", framework.graph().roots().get(0).children().get(0).literal());
        assertEquals("set", framework.graph().roots().get(0).children().get(0).children().get(0).literal());
        assertEquals("target", framework.graph().roots().get(0).children().get(0).children().get(0).arguments().get(0).name());
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
    void parsesSingleQuotedStringArgumentAsOneValue() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say 'hello world'");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("hello world"), result.reply());
    }

    @Test
    void parsesEscapedQuotesAndBackslashes() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .greedyArgument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say \"hello \\\"Ada\\\"\" 'it\\'s ok' C:\\\\tools");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("hello \"Ada\" it's ok C:\\tools"), result.reply());
    }

    @Test
    void preservesBareBackslashesInPathLikeArguments() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult path = framework.dispatch(new CommandSource() {
        }, "say C:\\tools");
        CommandResult trailing = framework.dispatch(new CommandSource() {
        }, "say C:\\");

        assertEquals(CommandResult.Status.SUCCESS, path.status());
        assertEquals(Optional.of("C:\\tools"), path.reply());
        assertEquals(CommandResult.Status.SUCCESS, trailing.status());
        assertEquals(Optional.of("C:\\"), trailing.reply());
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
    void parsesAdditionalBuiltInArgumentTypes() {
        CommandFramework framework = CommandFramework.create();
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        framework.registry().command("types", command -> command
            .argument("longValue", long.class)
            .argument("doubleValue", Double.class)
            .argument("enabled", boolean.class)
            .argument("id", UUID.class)
            .argument("mode", Mode.class)
            .executes(ctx -> Results.success(
                ctx.arg("longValue", long.class)
                    + ":"
                    + ctx.arg("doubleValue", Double.class)
                    + ":"
                    + ctx.arg("enabled", boolean.class)
                    + ":"
                    + ctx.arg("id", UUID.class)
                    + ":"
                    + ctx.arg("mode", Mode.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "types 42 3.5 true " + id + " FAST");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("42:3.5:true:" + id + ":FAST"), result.reply());
    }

    @Test
    void failsWithStableMessagesForAdditionalBuiltInTypes() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("types", command -> command
            .argument("longValue", Long.class)
            .argument("doubleValue", Double.class)
            .argument("enabled", Boolean.class)
            .argument("id", UUID.class)
            .argument("mode", Mode.class)
            .executes(ctx -> Results.silent()));

        assertEquals(Optional.of("Invalid long for argument longValue: nope"),
            framework.dispatch(new CommandSource() {
            }, "types nope 3.5 true 123e4567-e89b-12d3-a456-426614174000 FAST").reply());
        assertEquals(Optional.of("Invalid double for argument doubleValue: nope"),
            framework.dispatch(new CommandSource() {
            }, "types 42 nope true 123e4567-e89b-12d3-a456-426614174000 FAST").reply());
        assertEquals(Optional.of("Invalid double for argument doubleValue: NaN"),
            framework.dispatch(new CommandSource() {
            }, "types 42 NaN true 123e4567-e89b-12d3-a456-426614174000 FAST").reply());
        assertEquals(Optional.of("Invalid double for argument doubleValue: Infinity"),
            framework.dispatch(new CommandSource() {
            }, "types 42 Infinity true 123e4567-e89b-12d3-a456-426614174000 FAST").reply());
        assertEquals(Optional.of("Invalid boolean for argument enabled: maybe"),
            framework.dispatch(new CommandSource() {
            }, "types 42 3.5 maybe 123e4567-e89b-12d3-a456-426614174000 FAST").reply());
        assertEquals(Optional.of("Invalid UUID for argument id: nope"),
            framework.dispatch(new CommandSource() {
            }, "types 42 3.5 true nope FAST").reply());
        assertEquals(Optional.of("Invalid enum for argument mode: slow"),
            framework.dispatch(new CommandSource() {
            }, "types 42 3.5 true 123e4567-e89b-12d3-a456-426614174000 slow").reply());
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
    void failsForTrailingEscapedWhitespace() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("say", command -> command
            .argument("message", String.class)
            .executes(ctx -> Results.success(ctx.arg("message", String.class))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "say hello\\ ");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("hello "), result.reply());
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
    void suggestionsHideRootCommandsWithoutPermission() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("public", command -> command.executes(ctx -> Results.silent()));
        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .executes(ctx -> Results.silent()));

        List<String> suggestions = framework.suggest(deniedSource(), "", 0);

        assertEquals(List.of("public"), suggestions);
    }

    @Test
    void suggestionsHideSubcommandsWithoutPermission() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .subcommand("info", info -> info.executes(ctx -> Results.silent()))
            .subcommand("delete", delete -> delete
                .permission("admin.delete")
                .executes(ctx -> Results.silent())));

        List<String> suggestions = framework.suggest(deniedSource(), "user ", 5);

        assertEquals(List.of("info"), suggestions);
    }

    @Test
    void suggestionsHideFlagsWhenCurrentCommandIsNotPermitted() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .flag("force")
            .option("reason", String.class)
            .executes(ctx -> Results.silent()));

        List<String> suggestions = framework.suggest(deniedSource(), "secure --", 9);

        assertEquals(List.of(), suggestions);
    }

    @Test
    void suggestionsHideArgumentedRoutesWhenDescendantPermissionIsMissing() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user <target:int> rank set <rank:String>")
            .permission("user.rank.set")
            .executes(ctx -> Results.success("ok"));

        assertEquals(List.of(), framework.suggest(deniedSource(), "", 0));
        assertEquals(List.of(), framework.suggest(deniedSource(), "user 7 rank ", 12));
    }

    @Test
    void suggestionsIncludeFlagsForPermittedArgumentedRoutes() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user <target:int> rank set <rank:String> [--force]")
            .permission("user.rank.set")
            .executes(ctx -> Results.success("ok"));

        assertEquals(List.of("--force"), framework.suggest(allowedSource(), "user 7 rank set --", 18));
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
    void routeDslDispatchesRootLiteralAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban|block <target:String>")
            .executes(ctx -> Results.success(ctx.arg("target", String.class)));

        CommandResult canonical = framework.dispatch(new CommandSource() {
        }, "ban Steve");
        CommandResult alias = framework.dispatch(new CommandSource() {
        }, "block Alex");

        assertEquals(Optional.of("Steve"), canonical.reply());
        assertEquals(Optional.of("Alex"), alias.reply());
        assertEquals("Usage: ban <target:String>", framework.help("block"));
    }

    @Test
    void routeDslDispatchesNestedLiteralAlias() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user rank|roles set|put <target:String>")
            .executes(ctx -> Results.success(ctx.arg("target", String.class)));

        CommandResult canonical = framework.dispatch(new CommandSource() {
        }, "user rank set Ada");
        CommandResult alias = framework.dispatch(new CommandSource() {
        }, "user roles put Grace");

        assertEquals(Optional.of("Ada"), canonical.reply());
        assertEquals(Optional.of("Grace"), alias.reply());
        assertEquals("Usage: user rank set <target:String>", framework.help("user roles put"));
    }

    @Test
    void manualApiRegistersCommandTree() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().register(Commands.literal("ban")
            .alias("block")
            .argument(Arguments.required("target", String.class))
            .argument(Arguments.greedyOptional("reason", String.class))
            .flag(Flags.bool("silent").alias("s"))
            .handler(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + ":" + ctx.argOr("reason", "")
                    + ":" + ctx.flag("silent")))
            .build());

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "block Steve griefing -s");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Steve:griefing:true"), result.reply());
        assertEquals("Usage: ban <target:String> [reason:String...] [--silent|-s]", framework.help("ban"));
    }

    @Test
    void manualApiRegistersNestedCommandTreeWithValueOption() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().register(Commands.literal("user")
            .child(Commands.literal("rank")
                .child(Commands.literal("set")
                    .argument(Arguments.required("target", String.class))
                    .flag(Flags.option("amount", Integer.class).alias("a"))
                    .handler(ctx -> Results.success(ctx.arg("target", String.class)
                        + ":" + ctx.option("amount", Integer.class).orElse(0)))
                    .build())
                .build())
            .build());

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank set Alex -a 4");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Alex:4"), result.reply());
        assertEquals("Usage: user rank set <target:String> [--amount:Integer|-a]", framework.help("user rank set"));
    }

    @Test
    void manualApiRejectsInvalidTreeShapesAtRegistration() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().register(Commands.literal("bad")
                .alias("bad")
                .build()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().register(Commands.literal("bad")
                .argument(Arguments.optional("maybe", String.class))
                .argument(Arguments.required("required", String.class))
                .build()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().register(Commands.literal("bad")
                .argument(Arguments.greedy("message", String.class))
                .argument(Arguments.required("tail", String.class))
                .build()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().register(Commands.literal("bad")
                .flag(Flags.bool("silent"))
                .flag(Flags.option("silent", String.class))
                .build()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().register(Commands.literal("bad")
                .child(Commands.literal("run").build())
                .child(Commands.literal("run").build())
                .build()));
    }

    @Test
    void manualApiFailsWhenValueOptionIsMissingAtDispatch() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().register(Commands.literal("give")
            .flag(Flags.option("amount", Integer.class))
            .handler(ctx -> Results.silent())
            .build());

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "give --amount");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing value for option: amount"), result.reply());
    }

    @Test
    void manualApiNodesWithoutHandlersCanLaterMergeWithExecutableRoutes() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().register(Commands.literal("user")
            .child(Commands.literal("rank").build())
            .build());
        framework.registry()
            .route("user rank")
            .executes(ctx -> Results.success("rank"));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("rank"), result.reply());
    }

    @Test
    void flagSpecRejectsNonBooleanBooleanFlags() {
        assertThrows(IllegalArgumentException.class,
            () -> new dev.riege.buildmycommand.api.FlagSpec<>("dry", String.class, null,
                dev.riege.buildmycommand.api.FlagSpec.Kind.FLAG));
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
    void routeDslSupportsAdditionalBuiltInTypes() {
        CommandFramework framework = CommandFramework.create();
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        framework.registry()
            .route("types <longValue:long> <enabled:boolean> <id:UUID> [--ratio:double]")
            .executes(ctx -> Results.success(
                ctx.arg("longValue", long.class)
                    + ":"
                    + ctx.arg("enabled", boolean.class)
                    + ":"
                    + ctx.arg("id", UUID.class)
                    + ":"
                    + ctx.option("ratio", double.class).orElse(1.0)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "types 7 false " + id + " --ratio 2.5");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("7:false:" + id + ":2.5"), result.reply());
    }

    @Test
    void routeDslRejectsInvalidPatternAtRegistration() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("ban <target:Unknown>").executes(ctx -> Results.silent()));
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
    void sourceAwareHelpDeniesPermissionedCommandHelpWhenPermissionIsMissing() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .description("Secure command")
            .executes(ctx -> Results.silent()));

        assertEquals("Missing permission: admin.secure", framework.help(deniedSource(), "secure"));
    }

    @Test
    void sourceAwareHelpAllowsPermissionedCommandHelpWhenPermissionIsGranted() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .description("Secure command")
            .executes(ctx -> Results.silent()));

        assertEquals("""
            Usage: secure
            Description: Secure command""", framework.help(allowedSource(), "secure"));
        assertEquals("""
            Usage: secure
            Description: Secure command""", framework.help("secure"));
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

    @Test
    void helpIncludesCommandDescriptionWhenPresent() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ban", command -> command
            .description("Ban a user")
            .argument("target", String.class)
            .executes(ctx -> Results.silent()));

        assertEquals("""
            Usage: ban <target:String>
            Description: Ban a user""", framework.help("ban"));
    }

    @Test
    void metadataDoesNotChangeDispatchSemantics() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .description("Secure command")
            .permission("admin.secure")
            .executes(ctx -> Results.success("ok")));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "secure");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("ok"), result.reply());
    }

    @Test
    void deniesDispatchWhenSourceLacksCommandPermission() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .executes(ctx -> Results.success("ok")));

        CommandResult result = framework.dispatch(deniedSource(), "secure");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing permission: admin.secure"), result.reply());
    }

    @Test
    void deniesDispatchBeforeParsingPermissionedCommandArgumentsAndOptions() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .argument("name", String.class)
            .flag("force")
            .executes(ctx -> Results.success("ok")));

        CommandResult missingArgument = framework.dispatch(deniedSource(), "secure");
        CommandResult unknownOption = framework.dispatch(deniedSource(), "secure bob --bad");

        assertEquals(CommandResult.Status.FAILURE, missingArgument.status());
        assertEquals(Optional.of("Missing permission: admin.secure"), missingArgument.reply());
        assertEquals(CommandResult.Status.FAILURE, unknownOption.status());
        assertEquals(Optional.of("Missing permission: admin.secure"), unknownOption.reply());
    }

    @Test
    void deniesDispatchWhenMatchedParentPermissionIsMissing() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("admin", admin -> admin
            .permission("admin.root")
            .subcommand("child", child -> child.executes(ctx -> Results.success("ok"))));

        CommandResult result = framework.dispatch(deniedSource(), "admin child");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing permission: admin.root"), result.reply());
    }

    @Test
    void deniesDispatchBeforeParsingParentArgumentsForPermissionedDescendantRoute() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("user <target:int> rank set <rank:String>")
            .permission("user.rank.set")
            .executes(ctx -> Results.success("ok"));

        CommandResult result = framework.dispatch(deniedSource(), "user nope rank set admin");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Missing permission: user.rank.set"), result.reply());
    }

    @Test
    void allowsDispatchWhenSourceHasCommandPermission() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .executes(ctx -> Results.success("ok")));

        CommandResult result = framework.dispatch(allowedSource(), "secure");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("ok"), result.reply());
    }

    @Test
    void schemaIncludesBuilderManualAndRouteMetadata() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ban", command -> command
            .description("Ban a user")
            .permission("mod.ban")
            .executes(ctx -> Results.silent()));
        framework.registry().register(Commands.literal("reload")
            .description("Reload configuration")
            .permission("admin.reload")
            .handler(ctx -> Results.silent())
            .build());
        framework.registry()
            .route("user rank")
            .description("Manage ranks")
            .permission("user.rank")
            .executes(ctx -> Results.silent());

        assertEquals("""
            command ban
              description Ban a user
              permission mod.ban
            command reload
              description Reload configuration
              permission admin.reload
            command user
              child rank
            command user rank
              description Manage ranks
              permission user.rank""", framework.schema());
    }

    private static CommandSource deniedSource() {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return false;
            }
        };
    }

    private static CommandSource allowedSource() {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }

    private enum Mode {
        FAST,
        SAFE
    }
}
