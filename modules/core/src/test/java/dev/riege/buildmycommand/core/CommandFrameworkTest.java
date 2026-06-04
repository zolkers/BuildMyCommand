/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core;

import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.ArgumentParser;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.MessageLevel;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionProvider;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.api.Arguments;
import dev.riege.buildmycommand.api.CommandErrorHandler;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandLifecycleListener;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.FlagSpec;
import dev.riege.buildmycommand.api.Flags;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.middleware.CooldownMiddleware;
import dev.riege.buildmycommand.core.middleware.MiddlewareChain;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    void dispatchPreservesUniversalCommandInputInContext() {
        CommandFramework framework = CommandFramework.create();
        CommandPlatform platform = new CommandPlatform("minecraft", "Minecraft", true, true, true);
        CommandSource source = new CommandSource() {
        };
        AtomicReference<CommandInput> seenInput = new AtomicReference<>();

        framework.registry()
            .route("ping <target:String>")
            .executes(ctx -> {
                seenInput.set(ctx.commandInput());
                return Results.success(ctx.arg("target", String.class));
            });

        CommandInput input = new CommandInput(source, "/ping Alex", "ping Alex", 7, "/", platform);
        CommandResult result = framework.dispatch(input);

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Alex"), result.reply());
        assertEquals("ping Alex", seenInput.get().normalizedInput());
        assertEquals(input, seenInput.get());
        assertEquals(source, seenInput.get().source());
        assertEquals("/ping Alex", seenInput.get().rawInput());
        assertEquals("/ping Alex", seenInput.get().raw());
        assertEquals("ping Alex", seenInput.get().normalizedInput());
        assertEquals(7, seenInput.get().cursor());
        assertEquals("/", seenInput.get().prefix());
        assertEquals(platform, seenInput.get().platform());
    }

    @Test
    void dispatchSourceStringBuildsCompatibleCommandInputContext() {
        CommandFramework framework = CommandFramework.create();
        CommandSource source = new CommandSource() {
        };
        AtomicReference<CommandInput> seenInput = new AtomicReference<>();

        framework.registry().command("ping", command -> command.executes(ctx -> {
            seenInput.set(ctx.commandInput());
            return Results.success(ctx.input());
        }));

        CommandResult result = framework.dispatch(source, "ping");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("ping"), result.reply());
        assertEquals(source, seenInput.get().source());
        assertEquals("ping", seenInput.get().raw());
        assertEquals("ping", seenInput.get().rawInput());
        assertEquals("ping", seenInput.get().normalizedInput());
        assertEquals(4, seenInput.get().cursor());
        assertEquals("", seenInput.get().prefix());
        assertEquals(CommandPlatform.test(), seenInput.get().platform());
    }

    @Test
    void commandResultCarriesMessageWhileKeepingStringReplyCompatibility() {
        CommandResult success = Results.success("ok");
        CommandResult failure = Results.failure("bad");
        CommandResult silent = Results.silent();
        CommandResult legacy = new CommandResult(CommandResult.Status.SUCCESS, Optional.of("legacy"));

        assertEquals(CommandResult.Status.SUCCESS, success.status());
        assertEquals(Optional.of("ok"), success.reply());
        assertEquals(Optional.of(CommandMessage.success("ok")), success.message());
        assertEquals(CommandResult.Status.FAILURE, failure.status());
        assertEquals(Optional.of("bad"), failure.reply());
        assertEquals(Optional.of(CommandMessage.error("bad")), failure.message());
        assertEquals(CommandResult.Status.SILENT, silent.status());
        assertEquals(Optional.empty(), silent.reply());
        assertEquals(Optional.empty(), silent.message());
        assertEquals(CommandResult.Status.SUCCESS, legacy.status());
        assertEquals(Optional.of("legacy"), legacy.reply());
        assertEquals(Optional.of(CommandMessage.success("legacy")), legacy.message());
    }

    @Test
    void commandSourceDefaultsExposeIdentityMetadataAndMessageReply() {
        CommandSource source = new CommandSource() {
        };
        CommandMessage message = new CommandMessage("hello", MessageLevel.INFO, Map.of("audience", "test"));

        source.reply(message);

        assertEquals(Optional.empty(), source.id());
        assertEquals(Optional.empty(), source.name());
        assertEquals(Locale.ROOT, source.locale());
        assertEquals(Optional.empty(), source.unwrap(String.class));
        assertEquals(Optional.empty(), source.metadata("missing"));
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
    void suggestionsIncludeRootAndNestedAliases() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban|block rank|roles set|put <target:String>")
            .executes(ctx -> Results.success("ok"));

        assertEquals(List.of("ban", "block"), framework.suggest(new CommandSource() {
        }, "b", 1));
        assertEquals(List.of("rank", "roles"), framework.suggest(new CommandSource() {
        }, "ban r", 5));
        assertEquals(List.of("set", "put"), framework.suggest(new CommandSource() {
        }, "ban rank ", 9));
    }

    @Test
    void suggestionsIncludeLongAndShortOptionLabels() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("give <target:String> [--silent|-s] [--amount:Integer|-a]")
            .executes(ctx -> Results.success("ok"));

        assertEquals(List.of("--silent", "-s", "--amount", "-a"), framework.suggest(new CommandSource() {
        }, "give Ada -", 10));
        assertEquals(List.of("-s"), framework.suggest(new CommandSource() {
        }, "give Ada -s", 11));
        assertEquals(List.of("--amount"), framework.suggest(new CommandSource() {
        }, "give Ada --a", 12));
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
    void exposesRootLabelsIncludingAliasesForPlatformRegistration() {
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("ban|block <target:String>")
            .executes(ctx -> Results.silent());

        assertEquals(List.of("ban"), framework.rootLiterals());
        assertEquals(List.of("ban", "block"), framework.rootLabels());
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
    void dispatchIsCaseSensitiveByDefault() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "PING");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unknown command: PING"), result.reply());
    }

    @Test
    void caseInsensitiveLiteralsMatchCommandsSubcommandsAndAliasesWithoutChangingArguments() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();

        framework.registry()
            .route("ban|block <target:String> rank|roles set|put <rank:String>")
            .executes(ctx -> Results.success(ctx.arg("target", String.class) + ":" + ctx.arg("rank", String.class)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "BLOCK AdaCase Roles PuT AdminCase");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("AdaCase:AdminCase"), result.reply());
        assertEquals("Usage: ban <target:String> rank set <rank:String>", framework.help("BLOCK Roles PuT"));
    }

    @Test
    void caseInsensitiveOptionsMatchLongAndShortOptionNames() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveOptions()
            .build();

        framework.registry()
            .route("give <target:String> [--silent|-s] [--amount:Integer|-a]")
            .executes(ctx -> Results.success(
                ctx.arg("target", String.class)
                    + ":" + ctx.flag("silent")
                    + ":" + ctx.option("amount", Integer.class).orElse(0)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "give Ada -S --Amount 4");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Ada:true:4"), result.reply());
    }

    @Test
    void caseInsensitiveLiteralsApplyToManualCommandTrees() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();

        framework.registry().register(Commands.literal("User")
            .alias("u")
            .child(Commands.literal("Rank")
                .alias("roles")
                .child(Commands.literal("Set")
                    .alias("put")
                    .argument(Arguments.required("target", String.class))
                    .handler(ctx -> Results.success(ctx.arg("target", String.class)))
                    .build())
                .build())
            .build());

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "u roles put AdaCase");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("AdaCase"), result.reply());
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
    void failsWhenArgumentPrefixCannotBeParsedBeforeSubcommands() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("rank", command -> command
            .argument("level", Integer.class)
            .subcommand("set", set -> set.executes(ctx -> Results.success("set")))
            .executes(ctx -> Results.success(String.valueOf(ctx.arg("level", Integer.class)))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "rank nope");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Invalid integer for argument level: nope"), result.reply());
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
    void customPublicParserParsesRejectsAndReceivesParseContext() {
        AtomicReference<ArgumentParseContext> seenContext = new AtomicReference<>();
        CommandSource source = new CommandSource() {
        };
        CommandPlatform platform = new CommandPlatform("minecraft", "Minecraft", true, true, true);
        ArgumentParser<Rank> rankParser = (raw, context) -> {
            seenContext.set(context);
            if ("admin".equals(raw)) {
                return ArgumentParseResult.success(new Rank(raw));
            }
            return ArgumentParseResult.failure("Unknown rank");
        };
        CommandFramework framework = CommandFramework.builder()
            .argumentParser(Rank.class, rankParser)
            .build();

        framework.registry().command("rank", command -> command
            .argument("value", Rank.class)
            .executes(ctx -> Results.success(ctx.arg("value", Rank.class).name())));

        CommandResult success = framework.dispatch(new CommandInput(
            source,
            "/rank admin",
            "rank admin",
            11,
            "/",
            platform
        ));

        assertEquals(CommandResult.Status.SUCCESS, success.status());
        assertEquals(Optional.of("admin"), success.reply());
        assertEquals("admin", seenContext.get().rawToken());
        assertEquals(source, seenContext.get().source());
        assertEquals("rank admin", seenContext.get().input().normalizedInput());
        assertEquals("value", seenContext.get().name());
        assertEquals(Rank.class, seenContext.get().type());

        CommandResult failure = framework.dispatch(source, "rank owner");

        assertEquals(Optional.of("Unknown rank for argument value: owner"), failure.reply());
    }

    @Test
    void customPublicParserSuggestionsFeedStringAndRichSuggestionApis() {
        CommandFramework framework = CommandFramework.builder()
            .argumentParser(Rank.class, new ArgumentParser<>() {
                @Override
                public ArgumentParseResult<Rank> parse(String rawToken, ArgumentParseContext context) {
                    if ("admin".equals(rawToken)) {
                        return ArgumentParseResult.success(new Rank(rawToken));
                    }
                    return ArgumentParseResult.failure("Unknown rank");
                }

                @Override
                public List<Suggestion> suggestions(ArgumentParseContext context) {
                    return List.of(
                        new Suggestion("admin", Optional.of("Full access"), context.replacementStart(),
                            context.replacementEnd(), SuggestionType.ARGUMENT, 100),
                        new Suggestion("mod", Optional.empty(), context.replacementStart(), context.replacementEnd(),
                            SuggestionType.ARGUMENT, 50),
                        new Suggestion("helper", Optional.empty(), context.replacementStart(), context.replacementEnd(),
                            SuggestionType.ARGUMENT, 10)
                    );
                }
            })
            .build();

        framework.registry().command("rank", command -> command
            .argument("value", Rank.class)
            .executes(ctx -> Results.success(ctx.arg("value", Rank.class).name())));

        CommandInput input = CommandInput.normalized(new CommandSource() {
        }, "rank ");
        List<Suggestion> richSuggestions = framework.suggestRich(input);

        assertEquals(List.of("admin", "mod", "helper"),
            framework.suggest(new CommandSource() {
            }, "rank ", 5));
        assertEquals(List.of(
            new Suggestion("admin", Optional.of("Full access"), 5, 5, SuggestionType.ARGUMENT, 100),
            new Suggestion("mod", Optional.empty(), 5, 5, SuggestionType.ARGUMENT, 50),
            new Suggestion("helper", Optional.empty(), 5, 5, SuggestionType.ARGUMENT, 10)
        ), richSuggestions);
    }

    @Test
    void customPublicParserSuggestionsCompleteOptionValuesWithRanges() {
        CommandFramework framework = CommandFramework.builder()
            .suggestionProvider(Rank.class, context -> List.of("admin", "mod", "helper"))
            .build();

        framework.registry().command("rank", command -> command
            .option("value", Rank.class, "r")
            .executes(ctx -> Results.success(ctx.option("value", Rank.class).map(Rank::name).orElse("none"))));

        assertEquals(List.of(
            new Suggestion("admin", Optional.empty(), 13, 14, SuggestionType.OPTION_VALUE, 0),
            new Suggestion("mod", Optional.empty(), 13, 14, SuggestionType.OPTION_VALUE, 0),
            new Suggestion("helper", Optional.empty(), 13, 14, SuggestionType.OPTION_VALUE, 0)
        ), framework.suggestRich(CommandInput.normalized(new CommandSource() {
        }, "rank --value a")));
    }

    @Test
    void parserArgumentSuggestionsRequireCommandPermission() {
        CommandFramework framework = CommandFramework.builder()
            .suggestionProvider(Rank.class, context -> List.of("admin", "mod", "helper"))
            .build();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .argument("rank", Rank.class)
            .executes(ctx -> Results.silent()));

        assertEquals(List.of(), framework.suggest(deniedSource(), "secure ", 7));
    }

    @Test
    void parserOptionValueSuggestionsRequireCommandPermission() {
        CommandFramework framework = CommandFramework.builder()
            .suggestionProvider(Rank.class, context -> List.of("admin", "mod", "helper"))
            .build();

        framework.registry().command("secure", command -> command
            .permission("admin.secure")
            .option("rank", Rank.class)
            .executes(ctx -> Results.silent()));

        assertEquals(List.of(), framework.suggest(deniedSource(), "secure --rank ", 14));
    }

    @Test
    void caseInsensitiveOptionsSuggestRichOptionValuesAfterUppercaseOptionName() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveOptions()
            .suggestionProvider(Rank.class, context -> List.of("admin", "mod", "helper"))
            .build();

        framework.registry().command("rank", command -> command
            .option("value", Rank.class, "v")
            .executes(ctx -> Results.silent()));

        assertEquals(List.of(
            new Suggestion("admin", Optional.empty(), 13, 13, SuggestionType.OPTION_VALUE, 0),
            new Suggestion("mod", Optional.empty(), 13, 13, SuggestionType.OPTION_VALUE, 0),
            new Suggestion("helper", Optional.empty(), 13, 13, SuggestionType.OPTION_VALUE, 0)
        ), framework.suggestRich(CommandInput.normalized(new CommandSource() {
        }, "rank --VALUE ")));
    }

    @Test
    void parsesNewBuiltInArgumentAndOptionTypesWithStableFailures() throws Exception {
        CommandFramework framework = CommandFramework.create();
        URL url = URI.create("https://example.com/docs").toURL();

        framework.registry().command("types", command -> command
            .argument("ratio", float.class)
            .argument("ttl", Duration.class)
            .argument("date", LocalDate.class)
            .argument("timestamp", LocalDateTime.class)
            .argument("path", Path.class)
            .argument("uri", URI.class)
            .option("url", URL.class)
            .executes(ctx -> Results.success(
                ctx.arg("ratio", float.class)
                    + ":" + ctx.arg("ttl", Duration.class)
                    + ":" + ctx.arg("date", LocalDate.class)
                    + ":" + ctx.arg("timestamp", LocalDateTime.class)
                    + ":" + ctx.arg("path", Path.class)
                    + ":" + ctx.arg("uri", URI.class)
                    + ":" + ctx.option("url", URL.class).orElseThrow())));

        CommandResult success = framework.dispatch(new CommandSource() {
        }, "types 1.5 PT30S 2026-06-02 2026-06-02T10:15:30 C:\\tools https://example.com --url " + url);
        CommandResult invalidFloat = framework.dispatch(new CommandSource() {
        }, "types nope PT30S 2026-06-02 2026-06-02T10:15:30 C:\\tools https://example.com");
        CommandResult invalidDuration = framework.dispatch(new CommandSource() {
        }, "types 1.5 soon 2026-06-02 2026-06-02T10:15:30 C:\\tools https://example.com");
        CommandResult invalidUrl = framework.dispatch(new CommandSource() {
        }, "types 1.5 PT30S 2026-06-02 2026-06-02T10:15:30 C:\\tools https://example.com --url ht!tp://bad");

        assertEquals(CommandResult.Status.SUCCESS, success.status());
        assertEquals(Optional.of("1.5:PT30S:2026-06-02:2026-06-02T10:15:30:C:\\tools:https://example.com:" + url),
            success.reply());
        assertEquals(Optional.of("Invalid float for argument ratio: nope"), invalidFloat.reply());
        assertEquals(Optional.of("Invalid duration for argument ttl: soon"), invalidDuration.reply());
        assertEquals(Optional.of("Invalid URL for option url: ht!tp://bad"), invalidUrl.reply());
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
    void dispatchesLiteralPathBuilderForDeepSubcommandTrees() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .path("rank set promote", promote -> promote
                .description("Promote a user")
                .argument("target", String.class)
                .argument("rank", String.class)
                .executes(ctx -> Results.success(ctx.arg("target", String.class) + "=" + ctx.arg("rank", String.class))))
            .path("rank set demote", demote -> demote.executes(ctx -> Results.success("demote"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "user rank set promote Victor admin");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("Victor=admin"), result.reply());
        assertEquals("Usage: user rank set promote <target:String> <rank:String>\n"
            + "Description: Promote a user", framework.help("user rank set promote").replace("\r\n", "\n"));
        assertEquals(List.of("promote", "demote"), framework.suggest(new CommandSource() {
        }, "user rank set ", 14));
    }

    @Test
    void dispatchesRelativeRouteDslBuilderUnderCommandRoot() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicInteger middlewareCalls = new AtomicInteger();
        CommandSource permittedSource = new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };

        framework.registry().command("wecc", wecc -> wecc
            .subRoute("bang|b <target:String> [--silent|-s]", bang -> bang
                .description("Bang a target")
                .suggestAliases(false)
                .argumentSuggestions("target", ctx -> List.of("Ada", "Linus"))
                .executes(ctx -> Results.success(
                    ctx.arg("target", String.class) + " silent=" + ctx.flag("silent"))))
            .subRoute("ping", ping -> ping.executes(ctx -> Results.success("pong")))
            .subRoute("secret", secret -> secret
                .hidden()
                .executes(ctx -> Results.success("secret")))
            .subRoute("meta <target:String> [--mode:String|-m] [--level:String]", meta -> meta
                .permission("wecc.meta")
                .usage("/wecc meta <target> [--mode <mode>]")
                .example("/wecc meta Ada --mode inspect")
                .cooldown(Duration.ofSeconds(1))
                .requirement("staff || owner")
                .group("diagnostics")
                .middleware((context, command, path, next) -> {
                    middlewareCalls.incrementAndGet();
                    return next.proceed(context);
                })
                .argumentSuggestions("target", "targets", ctx -> List.of("Ada"))
                .optionSuggestions("mode", "modes", ctx -> List.of("inspect"))
                .optionSuggestions("level", ctx -> List.of("debug"))
                .executes(ctx -> Results.success(
                    ctx.arg("target", String.class)
                        + " mode="
                        + ctx.option("mode", String.class).orElse("default")))));

        CommandResult canonical = framework.dispatch(new CommandSource() {
        }, "wecc bang Ada --silent");
        CommandResult alias = framework.dispatch(new CommandSource() {
        }, "wecc b Linus -s");
        CommandResult sibling = framework.dispatch(new CommandSource() {
        }, "wecc ping");
        CommandResult meta = framework.dispatch(permittedSource, "wecc meta Ada --mode inspect");

        assertEquals(Optional.of("Ada silent=true"), canonical.reply());
        assertEquals(Optional.of("Linus silent=true"), alias.reply());
        assertEquals(Optional.of("pong"), sibling.reply());
        assertEquals(Optional.of("Ada mode=inspect"), meta.reply());
        assertEquals(1, middlewareCalls.get());
        assertEquals("Usage: wecc bang <target:String> [--silent|-s]\n"
            + "Description: Bang a target", framework.help("wecc bang").replace("\r\n", "\n"));
        assertEquals(List.of("bang", "ping", "meta"), framework.suggest(new CommandSource() {
        }, "wecc ", 5));
        assertEquals(List.of("Ada", "Linus"), framework.suggest(new CommandSource() {
        }, "wecc bang ", 10));
        assertEquals(List.of("Ada"), framework.suggest(permittedSource, "wecc meta ", 10));
        assertEquals(List.of("inspect"), framework.suggest(permittedSource, "wecc meta Ada --mode ", 21));
        assertEquals(List.of("debug"), framework.suggest(permittedSource, "wecc meta Ada --level ", 22));
        assertEquals(List.of("Ada"), framework.suggestRich(CommandInput.normalized(permittedSource, "wecc meta "))
            .stream()
            .map(Suggestion::value)
            .toList());
        assertEquals("Usage: /wecc meta <target> [--mode <mode>]\n"
                + "Example: /wecc meta Ada --mode inspect",
            framework.help(permittedSource, "wecc meta").replace("\r\n", "\n"));

        assertThrows(NullPointerException.class, () -> framework.registry().command("bad", bad ->
            bad.subRoute(null)));
        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("bad", bad ->
            bad.subRoute(" ")));
        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("bad", bad ->
            bad.subRoute("<target:String>")));
        assertThrows(IllegalArgumentException.class, () -> framework.registry().command("bad", bad ->
            bad.subRoute("leaf [optional:String] <required:String>").executes(ctx -> Results.silent())));

        Class<?> providerType = Class.forName(
            "dev.riege.buildmycommand.core.registry.SimpleCommandBuilder$NamedSuggestionProvider");
        java.lang.reflect.Constructor<?> constructor = providerType
            .getDeclaredConstructor(String.class, SuggestionProvider.class);
        constructor.setAccessible(true);
        SuggestionProvider provider = (SuggestionProvider) constructor
            .newInstance("direct", (SuggestionProvider) ctx -> List.of("direct"));
        assertEquals(List.of("direct"), provider.suggestions(null));
    }

    @Test
    void rejectsDslTokensInLiteralPathBuilder() {
        CommandFramework framework = CommandFramework.create();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> framework.registry().command("user", user -> user.path("rank <target:String>", target -> {
            })));

        assertEquals("literal path can only contain literal tokens; use route DSL for arguments/options: rank <target:String>",
            exception.getMessage());
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
            () -> new FlagSpec<>("dry", String.class, null, FlagSpec.Kind.FLAG));
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
            .route("types <longValue:long> <enabled:boolean> <ttl:Duration> <id:UUID> [--ratio:float]")
            .executes(ctx -> Results.success(
                ctx.arg("longValue", long.class)
                    + ":"
                    + ctx.arg("enabled", boolean.class)
                    + ":"
                    + ctx.arg("ttl", Duration.class)
                    + ":"
                    + ctx.arg("id", UUID.class)
                    + ":"
                    + ctx.option("ratio", float.class).orElse(1.0f)));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "types 7 false PT5S " + id + " --ratio 2.5");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("7:false:PT5S:" + id + ":2.5"), result.reply());
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
    void routeDslRejectsAnalysisOnlyTypesAtRuntimeRegistration() {
        CommandFramework framework = CommandFramework.create();

        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("give <amount:int{1..64}>").executes(ctx -> Results.silent()));
        assertThrows(IllegalArgumentException.class,
            () -> framework.registry().route("mode <mode:enum(EASY,NORMAL,HARD)>").executes(ctx -> Results.silent()));
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
    void globalMiddlewareCanBlockCommandExecution() {
        AtomicInteger executions = new AtomicInteger();
        CommandFramework framework = CommandFramework.builder()
            .middleware((context, command, path, next) -> Results.failure("blocked"))
            .build();

        framework.registry().command("ping", command -> command.executes(ctx -> {
            executions.incrementAndGet();
            return Results.success("Pong");
        }));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ping");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("blocked"), result.reply());
        assertEquals(0, executions.get());
    }

    @Test
    void globalMiddlewareWrapsExecutionInRegistrationOrder() {
        List<String> events = new ArrayList<>();
        CommandMiddleware first = (context, command, path, next) -> {
            events.add("first-before");
            CommandResult result = next.proceed(context);
            events.add("first-after");
            return result;
        };
        CommandMiddleware second = (context, command, path, next) -> {
            events.add("second-before");
            CommandResult result = next.proceed(context);
            events.add("second-after");
            return result;
        };
        CommandFramework framework = CommandFramework.builder()
            .middleware(first)
            .middleware(second)
            .build();

        framework.registry().command("ping", command -> command.executes(ctx -> {
            events.add("executor");
            return Results.success("Pong");
        }));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ping");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(List.of("first-before", "second-before", "executor", "second-after", "first-after"), events);
    }

    @Test
    void commandMetadataMiddlewareWrapsMatchedPathAfterGlobalMiddleware() {
        List<String> events = new ArrayList<>();
        CommandMiddleware global = (context, command, path, next) -> {
            events.add("global:" + String.join("/", path));
            CommandResult result = next.proceed(context);
            events.add("global-after");
            return result;
        };
        CommandMiddleware root = (context, command, path, next) -> {
            events.add("root:" + command.literal());
            CommandResult result = next.proceed(context);
            events.add("root-after");
            return result;
        };
        CommandMiddleware leaf = (context, command, path, next) -> {
            events.add("leaf:" + context.arg("target", String.class));
            CommandResult result = next.proceed(context);
            events.add("leaf-after");
            return result;
        };
        CommandFramework framework = CommandFramework.builder()
            .middleware(global)
            .build();

        framework.registry().command("admin", command -> command
            .middleware(root)
            .subcommand("ban", ban -> ban
                .middleware(leaf)
                .argument("target", String.class)
                .executes(ctx -> {
                    events.add("executor");
                    return Results.success("ok");
                }))
            .subcommand("status", status -> status.executes(ctx -> Results.success("status"))));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "admin ban Ada");
        CommandResult sibling = framework.dispatch(new CommandSource() {
        }, "admin status");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(CommandResult.Status.SUCCESS, sibling.status());
        assertEquals(List.of(
            "global:admin/ban",
            "root:ban",
            "leaf:Ada",
            "executor",
            "leaf-after",
            "root-after",
            "global-after",
            "global:admin/status",
            "root:status",
            "root-after",
            "global-after"
        ), events);
    }

    @Test
    void routeBuilderMiddlewareAppliesOnlyToExecutableRoute() {
        List<String> events = new ArrayList<>();
        CommandFramework framework = CommandFramework.create();

        framework.registry()
            .route("admin reload")
            .middleware((context, command, path, next) -> {
                events.add("reload:" + String.join("/", path));
                return next.proceed(context);
            })
            .executes(ctx -> Results.success("reload"));
        framework.registry()
            .route("admin status")
            .executes(ctx -> Results.success("status"));

        assertEquals(Optional.of("reload"), framework.dispatch(new CommandSource() {
        }, "admin reload").reply());
        assertEquals(Optional.of("status"), framework.dispatch(new CommandSource() {
        }, "admin status").reply());
        assertEquals(List.of("reload:admin/reload"), events);
    }

    @Test
    void middlewareChainLegacyExecuteUsesGlobalMiddlewareOnly() {
        List<String> events = new ArrayList<>();
        CommandMiddleware global = (context, command, path, next) -> {
            events.add("global:" + String.join("/", path));
            CommandResult result = next.proceed(context);
            events.add("global-after");
            return result;
        };
        MiddlewareChain chain = new MiddlewareChain(List.of(global));
        CommandContext context = new CommandContext(new CommandSource() {
        }, CommandInput.raw(new CommandSource() {
        }, "ping"), Map.of());
        CommandNode command = Commands.literal("ping")
            .handler(ctx -> Results.success("unreachable"))
            .build();

        CommandResult result = chain.execute(context, command, List.of("ping"), nextContext -> {
            events.add("terminal");
            return Results.success("ok");
        });

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("ok"), result.reply());
        assertEquals(List.of("global:ping", "terminal", "global-after"), events);
    }

    @Test
    void manualCommandNodeMetadataMiddlewareIsPreservedThroughRegistryImport() {
        List<String> events = new ArrayList<>();
        CommandMiddleware middleware = (context, command, path, next) -> {
            events.add("manual:" + String.join("/", path));
            return next.proceed(context);
        };
        CommandFramework framework = CommandFramework.create();

        framework.registry().register(Commands.literal("manual")
            .metadata(new CommandMetadata.Builder().middleware(middleware).build())
            .handler(ctx -> Results.success("ok"))
            .build());

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "manual");

        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of("ok"), result.reply());
        assertEquals(List.of("manual:manual"), events);
        assertEquals(1, framework.graph().roots().get(0).metadata().middlewares().size());
    }

    @Test
    void cooldownDenyRepeatedCommandForSameSourceAndCommandPath() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-02T10:00:00Z"));
        AtomicInteger executions = new AtomicInteger();
        CommandFramework framework = CommandFramework.builder()
            .cooldownClock(clock)
            .build();
        CommandSource ada = sourceWithId("ada");
        CommandSource bob = sourceWithId("bob");

        framework.registry()
            .route("ping|p")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> {
                executions.incrementAndGet();
                return Results.success("Pong");
            });

        CommandResult first = framework.dispatch(ada, "p");
        CommandResult repeated = framework.dispatch(ada, "ping");
        CommandResult otherSource = framework.dispatch(bob, "ping");
        clock.advance(Duration.ofSeconds(5));
        CommandResult afterCooldown = framework.dispatch(ada, "ping");

        assertEquals(CommandResult.Status.SUCCESS, first.status());
        assertEquals(CommandResult.Status.FAILURE, repeated.status());
        assertEquals(Optional.of("Command is on cooldown for PT5S"), repeated.reply());
        assertEquals(CommandResult.Status.SUCCESS, otherSource.status());
        assertEquals(CommandResult.Status.SUCCESS, afterCooldown.status());
        assertEquals(3, executions.get());
    }

    @Test
    void customErrorHandlerMapsExecutorAndMiddlewareExceptionsToFailureResults() {
        CommandErrorHandler handler = (context, command, path, error) ->
            Results.failure(String.join("/", path) + ": " + error.getMessage());
        CommandFramework executorFailure = CommandFramework.builder()
            .errorHandler(handler)
            .build();
        CommandFramework middlewareFailure = CommandFramework.builder()
            .middleware((context, command, path, next) -> {
                throw new IllegalStateException("middleware exploded");
            })
            .errorHandler(handler)
            .build();

        executorFailure.registry().command("boom", command -> command.executes(ctx -> {
            throw new IllegalArgumentException("executor exploded");
        }));
        middlewareFailure.registry().command("boom", command -> command.executes(ctx -> Results.success("unreachable")));

        CommandResult executorResult = executorFailure.dispatch(new CommandSource() {
        }, "boom");
        CommandResult middlewareResult = middlewareFailure.dispatch(new CommandSource() {
        }, "boom");

        assertEquals(CommandResult.Status.FAILURE, executorResult.status());
        assertEquals(Optional.of("boom: executor exploded"), executorResult.reply());
        assertEquals(CommandResult.Status.FAILURE, middlewareResult.status());
        assertEquals(Optional.of("boom: middleware exploded"), middlewareResult.reply());
    }

    @Test
    void customErrorHandlerFallsBackToDefaultDispatchFailures() {
        CommandFramework framework = CommandFramework.builder()
            .errorHandler((context, command, path, error) -> Results.failure("handled"))
            .build();

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "missing");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("Unknown command: missing"), result.reply());
    }

    @Test
    void lifecycleListenerObservesRegistrationsUpdatesAndRegistryRebuilds() {
        List<String> events = new ArrayList<>();
        CommandLifecycleListener listener = new CommandLifecycleListener() {
            @Override
            public void commandRegistered(CommandNode command, List<String> path) {
                events.add("registered:" + String.join("/", path));
            }

            @Override
            public void commandUpdated(CommandNode command, List<String> path) {
                events.add("updated:" + String.join("/", path));
            }

            @Override
            public void registryRebuilt(List<CommandNode> roots) {
                events.add("rebuilt:" + roots.stream().map(CommandNode::literal).toList());
            }
        };
        CommandFramework framework = CommandFramework.builder()
            .lifecycleListener(listener)
            .build();

        framework.registry().command("ping", command -> command.executes(ctx -> Results.silent()));
        framework.registry().route("admin reload").executes(ctx -> Results.silent());
        framework.registry().route("admin status").executes(ctx -> Results.silent());

        assertEquals(List.of(
            "registered:ping",
            "rebuilt:[ping]",
            "registered:admin/reload",
            "rebuilt:[ping, admin]",
            "registered:admin/status",
            "rebuilt:[ping, admin]"
        ), events);
    }

    @Test
    void unregisterRemovesNestedPathAndEmitsLifecycleEvents() {
        List<String> events = new ArrayList<>();
        CommandFramework framework = CommandFramework.builder()
            .lifecycleListener(new CommandLifecycleListener() {
                @Override
                public void commandUnregistered(List<String> path) {
                    events.add("unregistered:" + String.join("/", path));
                }

                @Override
                public void registryRebuilt(List<CommandNode> roots) {
                    events.add("rebuilt:" + roots.stream().map(CommandNode::literal).toList());
                }
            })
            .build();

        framework.registry().route("admin reload").executes(ctx -> Results.success("reload"));
        framework.registry().route("admin status").executes(ctx -> Results.success("status"));

        boolean removed = framework.registry().unregister("admin reload");
        CommandResult removedCommand = framework.dispatch(new CommandSource() {
        }, "admin reload");
        CommandResult sibling = framework.dispatch(new CommandSource() {
        }, "admin status");
        boolean removedAgain = framework.registry().unregister("admin reload");

        assertEquals(true, removed);
        assertEquals(CommandResult.Status.FAILURE, removedCommand.status());
        assertEquals(CommandResult.Status.SUCCESS, sibling.status());
        assertEquals(Optional.of("status"), sibling.reply());
        assertEquals(false, removedAgain);
        assertEquals(List.of(
            "rebuilt:[admin]",
            "rebuilt:[admin]",
            "unregistered:admin/reload",
            "rebuilt:[admin]"
        ), events);
    }

    @Test
    void unregisterRemovesRootAliasesAndRootCommand() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("ping|p")
            .executes(ctx -> Results.success("Pong"));

        boolean removed = framework.registry().unregister("p");
        CommandResult canonical = framework.dispatch(new CommandSource() {
        }, "ping");
        CommandResult alias = framework.dispatch(new CommandSource() {
        }, "p");

        assertEquals(true, removed);
        assertEquals(CommandResult.Status.FAILURE, canonical.status());
        assertEquals(CommandResult.Status.FAILURE, alias.status());
    }

    @Test
    void errorHandlerDoesNotCatchFatalErrors() {
        CommandFramework framework = CommandFramework.builder()
            .errorHandler((context, command, path, error) -> Results.failure("handled"))
            .build();
        AssertionError fatal = new AssertionError("fatal");

        framework.registry().command("boom", command -> command.executes(ctx -> {
            throw fatal;
        }));

        AssertionError thrown = assertThrows(AssertionError.class, () -> framework.dispatch(new CommandSource() {
        }, "boom"));

        assertEquals(fatal, thrown);
    }

    @Test
    void cooldownReservesBeforeExecutionForConcurrentDispatch() throws InterruptedException {
        CountDownLatch enteredExecutor = new CountDownLatch(1);
        CountDownLatch releaseExecutor = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<CommandResult> first = new AtomicReference<>();
        AtomicReference<CommandResult> second = new AtomicReference<>();
        CommandFramework framework = CommandFramework.create();
        CommandSource source = sourceWithId("ada");

        framework.registry()
            .route("slow")
            .cooldown(Duration.ofSeconds(30))
            .executes(ctx -> {
                executions.incrementAndGet();
                enteredExecutor.countDown();
                await(releaseExecutor);
                return Results.success("done");
            });

        Thread firstThread = new Thread(() -> first.set(framework.dispatch(source, "slow")));
        firstThread.start();
        await(enteredExecutor);

        Thread secondThread = new Thread(() -> second.set(framework.dispatch(source, "slow")));
        secondThread.start();
        secondThread.join(5_000);
        releaseExecutor.countDown();
        firstThread.join(5_000);

        assertEquals(CommandResult.Status.SUCCESS, first.get().status());
        assertEquals(CommandResult.Status.FAILURE, second.get().status());
        assertEquals(1, executions.get());
    }

    @Test
    void failedCooldownExecutionRollsBackReservationAndExpiredEntriesAreCleanedUp() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-02T10:00:00Z"));
        ConcurrentHashMap<CooldownMiddleware.Key, Instant> store = new ConcurrentHashMap<>();
        store.put(new CooldownMiddleware.Key("stale", "stale"), clock.instant().minusSeconds(1));
        AtomicInteger executions = new AtomicInteger();
        CommandFramework framework = CommandFramework.builder()
            .cooldownClock(clock)
            .cooldownStore(store)
            .build();

        framework.registry()
            .route("flaky")
            .cooldown(Duration.ofSeconds(5))
            .executes(ctx -> {
                if (executions.incrementAndGet() == 1) {
                    return Results.failure("nope");
                }
                return Results.success("ok");
            });

        CommandResult failure = framework.dispatch(sourceWithId("ada"), "flaky");
        CommandResult retry = framework.dispatch(sourceWithId("ada"), "flaky");

        assertEquals(CommandResult.Status.FAILURE, failure.status());
        assertEquals(CommandResult.Status.SUCCESS, retry.status());
        assertEquals(false, store.containsKey(new CooldownMiddleware.Key("stale", "stale")));
        assertEquals(2, executions.get());
    }

    @Test
    void middlewareNextProceedIsOneShotAndMappedByErrorHandler() {
        AtomicInteger executions = new AtomicInteger();
        CommandFramework framework = CommandFramework.builder()
            .middleware((context, command, path, next) -> {
                next.proceed(context);
                return next.proceed(context);
            })
            .errorHandler((context, command, path, error) -> Results.failure(error.getMessage()))
            .build();

        framework.registry().command("ping", command -> command.executes(ctx -> {
            executions.incrementAndGet();
            return Results.success("Pong");
        }));

        CommandResult result = framework.dispatch(new CommandSource() {
        }, "ping");

        assertEquals(CommandResult.Status.FAILURE, result.status());
        assertEquals(Optional.of("middleware next already called"), result.reply());
        assertEquals(1, executions.get());
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

    private static CommandSource sourceWithId(String id) {
        return new CommandSource() {
            @Override
            public Optional<String> id() {
                return Optional.of(id);
            }
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted waiting for latch", exception);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }

    private enum Mode {
        FAST,
        SAFE
    }

    private record Rank(String name) {
    }
}
