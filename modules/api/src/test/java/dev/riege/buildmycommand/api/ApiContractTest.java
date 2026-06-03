package dev.riege.buildmycommand.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ApiContractTest {
    @Test
    void argumentSpecsValidateNamesTypesAndKinds() {
        assertEquals(ArgumentSpec.Kind.REQUIRED, Arguments.required("name", String.class).kind());
        assertEquals(ArgumentSpec.Kind.OPTIONAL, Arguments.optional("name", String.class).kind());
        assertEquals(ArgumentSpec.Kind.GREEDY, Arguments.greedy("name", String.class).kind());
        assertEquals(ArgumentSpec.Kind.OPTIONAL_GREEDY, Arguments.greedyOptional("name", String.class).kind());

        assertThrows(NullPointerException.class, () -> new ArgumentSpec<>(null, String.class, ArgumentSpec.Kind.REQUIRED));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentSpec<>(" ", String.class,
            ArgumentSpec.Kind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new ArgumentSpec<>("name", null, ArgumentSpec.Kind.REQUIRED));
        assertThrows(NullPointerException.class, () -> new ArgumentSpec<>("name", String.class, null));
    }

    @Test
    void flagSpecsValidateAliasesAndKinds() {
        FlagSpec<Boolean> flag = Flags.bool("silent").alias("s");
        FlagSpec<Integer> option = Flags.option("amount", Integer.class).alias("a");

        assertEquals(FlagSpec.Kind.FLAG, flag.kind());
        assertEquals(Optional.of("s"), flag.aliasOptional());
        assertEquals(FlagSpec.Kind.VALUE, option.kind());
        assertEquals(Optional.of("a"), option.aliasOptional());
        assertEquals(Optional.empty(), Flags.option("amount", Integer.class).aliasOptional());

        assertThrows(NullPointerException.class, () -> new FlagSpec<>(null, Boolean.class, null, FlagSpec.Kind.FLAG));
        assertThrows(IllegalArgumentException.class, () -> new FlagSpec<>(" ", Boolean.class, null,
            FlagSpec.Kind.FLAG));
        assertThrows(NullPointerException.class, () -> new FlagSpec<>("silent", null, null, FlagSpec.Kind.FLAG));
        assertThrows(NullPointerException.class, () -> new FlagSpec<>("silent", Boolean.class, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FlagSpec<>("broken", String.class, null,
            FlagSpec.Kind.FLAG));
        assertThrows(IllegalArgumentException.class, () -> new FlagSpec<>("silent", boolean.class, " ",
            FlagSpec.Kind.FLAG));
    }

    @Test
    void commandContextReadsTypedArgumentsOptionsFlagsAndFallbacks() throws Exception {
        CommandSource source = new CommandSource() {
        };
        CommandInput input = CommandInput.normalized(source, "give Ada");
        CommandContext context = new CommandContext(source, input, Map.of(
            "name", "Ada",
            "amount", 3,
            "silent", true,
            "longValue", 4L,
            "doubleValue", 1.5D,
            "floatValue", 2.5F,
            "shortValue", (short) 7,
            "byteValue", (byte) 8,
            "charValue", 'x'
        ));

        assertEquals("Ada", context.arg("name", String.class));
        assertEquals(3, context.arg("amount", int.class));
        assertEquals(Optional.of("Ada"), context.optionalArg("name", String.class));
        assertEquals(Optional.empty(), context.optionalArg("missing", String.class));
        assertEquals(true, context.flag("silent"));
        assertEquals(false, context.flag("missing"));
        assertEquals(Optional.of(3), context.option("amount", Integer.class));
        assertEquals(Optional.empty(), context.option("missing", Integer.class));
        assertEquals("fallback", context.argOr("missing", "fallback"));
        assertEquals("Ada", context.argOr("name", "fallback"));
        assertEquals(true, context.arg("silent", boolean.class));
        assertEquals(4L, context.arg("longValue", long.class));
        assertEquals(1.5D, context.arg("doubleValue", double.class));
        assertEquals(2.5F, context.arg("floatValue", float.class));
        assertEquals((short) 7, context.arg("shortValue", short.class));
        assertEquals((byte) 8, context.arg("byteValue", byte.class));
        assertEquals('x', context.arg("charValue", char.class));
        assertSame(String.class, context.arg("name", String.class).getClass());
        Method primitiveWrapper = CommandContext.class.getDeclaredMethod("primitiveWrapper", Class.class);
        primitiveWrapper.setAccessible(true);
        assertSame(void.class, primitiveWrapper.invoke(null, void.class));
        assertEquals(source, new CommandContext(source, "raw").source());
        assertEquals("raw", new CommandContext(source, "raw", Map.of()).input());

        assertThrows(IllegalArgumentException.class, () -> context.arg("missing", String.class));
        assertThrows(ClassCastException.class, () -> context.flag("name"));
        assertThrows(NullPointerException.class, () -> context.arg(null, String.class));
        assertThrows(NullPointerException.class, () -> context.arg("name", null));
        assertThrows(NullPointerException.class, () -> new CommandContext(null, input, Map.of()));
        assertThrows(NullPointerException.class, () -> new CommandContext(source, (CommandInput) null, Map.of()));
        assertThrows(NullPointerException.class, () -> new CommandContext(source, input, null));
    }

    @Test
    void commandInputNormalizesPrefixesAndClampsCursors() {
        CommandSource source = new CommandSource() {
        };

        CommandInput prefixed = new CommandInput(source, "/ping", 2, "/", CommandPlatform.terminal());
        CommandInput negativeCursor = new CommandInput(source, "ping", -10, "", CommandPlatform.test());
        CommandInput hugeCursor = new CommandInput(source, "ping", 99, "/", CommandPlatform.test());

        assertEquals("ping", prefixed.normalizedInput());
        assertEquals(1, prefixed.normalizedCursor());
        assertEquals("ping", negativeCursor.raw());
        assertEquals(0, negativeCursor.cursor());
        assertEquals(0, negativeCursor.normalizedCursor());
        assertEquals(4, hugeCursor.cursor());
        assertEquals(4, hugeCursor.normalizedCursor());
        assertEquals("ping", CommandInput.raw(source, "ping").normalizedInput());

        assertThrows(NullPointerException.class, () -> new CommandInput(null, "ping", 0, "", CommandPlatform.test()));
        assertThrows(NullPointerException.class, () -> new CommandInput(source, null, 0, "", CommandPlatform.test()));
        assertThrows(NullPointerException.class, () -> new CommandInput(source, "ping", 0, null,
            CommandPlatform.test()));
        assertThrows(NullPointerException.class, () -> new CommandInput(source, "ping", 0, "", null));
    }

    @Test
    void commandMessagesResultsAndPlatformsValidateContracts() {
        CommandResult success = Results.success("ok");
        CommandResult failure = Results.failure("bad");
        CommandResult silent = Results.silent();
        CommandResult silentWithReply = new CommandResult(CommandResult.Status.SILENT, Optional.of("quiet"));
        CommandMessage warning = new CommandMessage("careful", MessageLevel.WARNING, Map.of("code", 1));
        CommandResult rich = CommandResult.message(CommandResult.Status.FAILURE, warning);

        assertEquals(Optional.of("ok"), success.reply());
        assertEquals(CommandResult.Status.FAILURE, rich.status());
        assertEquals(Optional.of(CommandMessage.success("ok")), success.message());
        assertEquals(Optional.of(CommandMessage.error("bad")), failure.message());
        assertEquals(Optional.empty(), silent.reply());
        assertEquals(Optional.of(CommandMessage.info("quiet")), silentWithReply.message());
        assertEquals(Optional.of(warning), rich.message());
        assertEquals(success, new CommandResult(CommandResult.Status.SUCCESS, Optional.of("ok")));
        assertEquals(success, success);
        assertNotEquals(success, failure);
        assertNotEquals(success, new CommandResult(CommandResult.Status.SUCCESS, Optional.of("other")));
        assertNotEquals(success, "ok");
        assertEquals(success.hashCode(), new CommandResult(CommandResult.Status.SUCCESS, Optional.of("ok")).hashCode());
        assertTrue(success.toString().contains("SUCCESS"));
        assertEquals(CommandMessage.info("info"), new CommandMessage("info", MessageLevel.INFO, Map.of()));
        assertEquals(CommandPlatform.test(), new CommandPlatform("test", "Test", false, true, true));
        assertEquals(CommandPlatform.terminal(), new CommandPlatform("terminal", "Terminal", false, true, true));

        assertThrows(NullPointerException.class, () -> Results.success(null));
        assertThrows(NullPointerException.class, () -> Results.failure(null));
        assertThrows(NullPointerException.class, () -> CommandResult.message(null, warning));
        assertThrows(NullPointerException.class, () -> CommandResult.message(CommandResult.Status.SUCCESS, null));
        assertThrows(NullPointerException.class, () -> new CommandMessage(null, MessageLevel.INFO, Map.of()));
        assertThrows(NullPointerException.class, () -> new CommandMessage("text", null, Map.of()));
        assertThrows(NullPointerException.class, () -> new CommandMessage("text", MessageLevel.INFO, null));
        assertThrows(IllegalArgumentException.class, () -> new CommandPlatform(" ", "Test", false, true, true));
        assertThrows(IllegalArgumentException.class, () -> new CommandPlatform("test", " ", false, true, true));
    }

    @Test
    void commandExceptionsExposeTypedFailureContractsAndMappingHandlers() {
        CommandSource source = new CommandSource() {
        };
        CommandInput input = CommandInput.raw(source, "ban Ada");
        CommandContext context = new CommandContext(source, input, Map.of("target", "Ada"));
        CommandNode command = Commands.literal("ban").handler(ctx -> Results.success("ok")).build();
        CommandExceptionContext executionContext =
            CommandExceptionContext.execution(input, context, command, List.of("ban"));

        assertEquals(input, executionContext.input());
        assertEquals(Optional.of(context), executionContext.context());
        assertEquals(Optional.of(command), executionContext.command());
        assertEquals(List.of("ban"), executionContext.commandPath());
        assertEquals(Optional.empty(), CommandExceptionContext.dispatch(input).context());
        UnknownCommandException unknown = new UnknownCommandException("missing");
        assertEquals("Unknown command: missing", unknown.getMessage());
        assertEquals("missing", unknown.command());
        assertEquals("Missing permission: ban.use", new PermissionDeniedException("ban.use").getMessage());
        assertEquals("Invalid integer", new ArgumentParseException("Invalid integer").getMessage());
        assertEquals("Missing value", new OptionParseException("Missing value").getMessage());
        assertEquals("Broken quotes", new CommandSyntaxException("Broken quotes").getMessage());
        RuntimeException cause = new RuntimeException("cause");
        assertEquals(cause, new CommandException("wrapped", cause).getCause());

        CommandExceptionHandler handler = CommandExceptionHandlers.mapping()
            .on(PermissionDeniedException.class, (failureContext, error) ->
                Results.failure("perm " + error.permission() + " on " + failureContext.input().normalizedInput()))
            .on(CommandException.class, (failureContext, error) -> Results.failure("typed " + error.getMessage()))
            .fallback((failureContext, error) -> Results.failure("fallback " + error.getMessage()))
            .build();

        assertEquals(Optional.of("perm ban.use on ban Ada"),
            handler.handle(executionContext, new PermissionDeniedException("ban.use")).reply());
        assertEquals(Optional.of("typed Unknown command: nope"),
            handler.handle(executionContext, new UnknownCommandException("nope")).reply());
        assertEquals(Optional.of("fallback boom"),
            handler.handle(executionContext, new IllegalStateException("boom")).reply());

        CommandExceptionHandler failureMessage = CommandExceptionHandlers.failureMessage();
        assertEquals(Optional.of("Missing permission: ban.use"),
            failureMessage.handle(executionContext, new PermissionDeniedException("ban.use")).reply());
        assertThrows(IllegalStateException.class, () ->
            CommandExceptionHandlers.rethrow().handle(executionContext, new IllegalStateException("boom")));
        assertThrows(AssertionError.class, () ->
            CommandExceptionHandlers.rethrow().handle(executionContext, new AssertionError("fatal")));
        RuntimeException wrappedChecked = assertThrows(RuntimeException.class, () ->
            CommandExceptionHandlers.rethrow().handle(executionContext, new Exception("checked")));
        assertEquals("checked", wrappedChecked.getCause().getMessage());
        assertThrows(RuntimeException.class, () ->
            failureMessage.handle(executionContext, new IllegalStateException("boom")));
        assertThrows(AssertionError.class, () ->
            failureMessage.handle(executionContext, new AssertionError("fatal fallback")));
        RuntimeException failureMessageChecked = assertThrows(RuntimeException.class, () ->
            failureMessage.handle(executionContext, new Exception("checked fallback")));
        assertEquals("checked fallback", failureMessageChecked.getCause().getMessage());
        try {
            failureMessage.handle(executionContext, new Throwable("plain fallback"));
            fail("plain throwable should be wrapped");
        } catch (RuntimeException error) {
            assertEquals("plain fallback", error.getCause().getMessage());
        }
        assertThrows(NullPointerException.class, () ->
            CommandExceptionHandlers.rethrow().handle(null, new CommandException("x")));
        assertThrows(NullPointerException.class, () ->
            CommandExceptionHandlers.rethrow().handle(executionContext, null));
        assertThrows(NullPointerException.class, () ->
            failureMessage.handle(null, new CommandException("x")));
        assertThrows(NullPointerException.class, () ->
            failureMessage.handle(executionContext, null));
        assertThrows(NullPointerException.class, () -> CommandExceptionContext.dispatch(null));
        assertThrows(NullPointerException.class, () -> CommandExceptionContext.execution(null, context, command,
            List.of("ban")));
        assertThrows(NullPointerException.class, () -> CommandExceptionContext.execution(input, null, command,
            List.of("ban")));
        assertThrows(NullPointerException.class, () -> CommandExceptionContext.execution(input, context, null,
            List.of("ban")));
        assertThrows(NullPointerException.class, () -> CommandExceptionContext.execution(input, context, command,
            null));
        assertThrows(IllegalArgumentException.class, () -> new CommandException(" "));
        assertThrows(NullPointerException.class, () -> new CommandException(null));
        assertThrows(NullPointerException.class, () -> new UnknownCommandException(null));
        assertThrows(NullPointerException.class, () -> new PermissionDeniedException(null));
        assertThrows(NullPointerException.class, () -> new ArgumentParseException(null));
        assertThrows(NullPointerException.class, () -> new OptionParseException(null));
        assertThrows(NullPointerException.class, () -> new CommandSyntaxException(null));
        assertThrows(NullPointerException.class, () -> CommandExceptionHandlers.mapping().on(null,
            (failureContext, error) -> Results.failure("x")));
        assertThrows(NullPointerException.class, () -> CommandExceptionHandlers.mapping()
            .on(CommandException.class, null));
        assertThrows(NullPointerException.class, () -> CommandExceptionHandlers.mapping().fallback(null));
        assertThrows(NullPointerException.class, () -> handler.handle(null, new CommandException("x")));
        assertThrows(NullPointerException.class, () -> handler.handle(executionContext, null));
    }

    @Test
    void commandMetadataBuilderValidatesAndSnapshotsValues() {
        List<String> examples = new ArrayList<>(List.of("one", "two"));
        List<CommandMiddleware> middlewares = new ArrayList<>();
        CommandMiddleware middleware = (context, command, path, next) -> next.proceed(context);
        middlewares.add(middleware);
        CommandMetadata metadata = new CommandMetadata.Builder()
            .hidden()
            .usage("/ban <target>")
            .example("ban Ada")
            .examples(examples)
            .cooldown(Duration.ofSeconds(5))
            .requirement("staff")
            .group("moderation")
            .suggestAliases(false)
            .middlewares(middlewares)
            .build();
        examples.add("mutated");
        middlewares.clear();

        assertTrue(metadata.hidden());
        assertEquals(Optional.of("/ban <target>"), metadata.usage());
        assertEquals(List.of("ban Ada", "one", "two"), metadata.examples());
        assertEquals(Optional.of(Duration.ofSeconds(5)), metadata.cooldown());
        assertEquals(Optional.of("staff"), metadata.requirement());
        assertEquals(Optional.of("moderation"), metadata.group());
        assertFalse(metadata.suggestAliases());
        assertEquals(List.of(middleware), metadata.middlewares());
        assertEquals(CommandMetadata.empty(), new CommandMetadata.Builder().build());
        assertTrue(CommandMetadata.empty().suggestAliases());

        assertThrows(NullPointerException.class, () -> new CommandMetadata.Builder().usage(null));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().usage(" "));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().example(""));
        assertThrows(NullPointerException.class, () -> new CommandMetadata.Builder().examples(null));
        assertThrows(NullPointerException.class, () -> new CommandMetadata.Builder().cooldown(null));
        assertThrows(NullPointerException.class, () -> new CommandMetadata.Builder().middleware(null));
        assertThrows(NullPointerException.class, () -> new CommandMetadata.Builder().middlewares(null));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().cooldown(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().cooldown(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().requirement(" "));
        assertThrows(IllegalArgumentException.class, () -> new CommandMetadata.Builder().group(" "));
        assertThrows(NullPointerException.class, () -> new CommandMetadata(false, null, List.of(), Optional.empty(),
            Optional.empty(), Optional.empty(), true, List.of()));
        assertThrows(NullPointerException.class, () -> new CommandMetadata(false, Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(), true, null));
    }

    @Test
    void commandNodeBuilderValidatesAndSnapshotsTree() {
        CommandRegistry.CommandExecutor executor = ctx -> Results.success("ok");
        CommandNode child = Commands.literal("child").handler(executor).build();
        CommandMetadata metadata = new CommandMetadata.Builder().usage("/root").build();
        CommandNode root = Commands.literal("root")
            .description("Root command")
            .permission("root.use")
            .alias("r")
            .aliases("main", "base")
            .argument(Arguments.required("name", String.class))
            .flag(Flags.bool("silent"))
            .child(child)
            .metadata(metadata)
            .handler(executor)
            .build();

        assertEquals("root", root.literal());
        assertEquals(Optional.of("Root command"), root.description());
        assertEquals(Optional.of("root.use"), root.permission());
        assertEquals(List.of("r", "main", "base"), root.aliases());
        assertEquals(List.of(child), root.children());
        assertEquals(Optional.of(executor), root.executor());
        assertEquals(metadata, root.metadata());

        assertThrows(NullPointerException.class, () -> Commands.literal(null));
        assertThrows(IllegalArgumentException.class, () -> Commands.literal(" ").build());
        assertThrows(IllegalArgumentException.class, () -> Commands.literal("root").description(" "));
        assertThrows(IllegalArgumentException.class, () -> Commands.literal("root").permission(""));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").alias(null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").aliases((String[]) null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").argument(null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").flag(null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").child(null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").metadata(null));
        assertThrows(NullPointerException.class, () -> Commands.literal("root").handler(null));
    }

    @Test
    void parseResultsSuggestionsAndProvidersValidateShape() {
        CommandSource source = new CommandSource() {
        };
        ArgumentParseContext context = new ArgumentParseContext(
            source,
            CommandInput.raw(source, "ping A"),
            "target",
            String.class,
            "A",
            5,
            6,
            SuggestionType.ARGUMENT
        );
        ArgumentParser<String> parser = (rawToken, parseContext) -> ArgumentParseResult.success(rawToken);
        SuggestionProvider provider = ctx -> List.of("Ada", "Alex");

        assertEquals(Optional.of("Ada"), ArgumentParseResult.success("Ada").value());
        assertEquals(Optional.of("bad"), ArgumentParseResult.failure("bad").failure());
        assertEquals(List.of(), parser.suggestions(context));
        assertEquals(List.of(
            new Suggestion("Ada", Optional.empty(), 5, 6, SuggestionType.ARGUMENT, 0),
            new Suggestion("Alex", Optional.empty(), 5, 6, SuggestionType.ARGUMENT, 0)
        ), provider.richSuggestions(context));

        assertThrows(NullPointerException.class, () -> ArgumentParseResult.success(null));
        assertThrows(NullPointerException.class, () -> ArgumentParseResult.failure(null));
        assertThrows(IllegalArgumentException.class, () -> ArgumentParseResult.failure(" "));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentParseResult<>(Optional.empty(),
            Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentParseResult<>(Optional.of("x"),
            Optional.of("bad")));
        assertThrows(IllegalArgumentException.class, () -> new Suggestion("x", Optional.empty(), -1, 0,
            SuggestionType.COMMAND, 0));
        assertThrows(IllegalArgumentException.class, () -> new Suggestion("x", Optional.empty(), 2, 1,
            SuggestionType.COMMAND, 0));
        assertThrows(NullPointerException.class, () -> new Suggestion(null, Optional.empty(), 0, 0,
            SuggestionType.COMMAND, 0));
        assertThrows(NullPointerException.class, () -> new Suggestion("x", null, 0, 0, SuggestionType.COMMAND, 0));
        assertThrows(NullPointerException.class, () -> new Suggestion("x", Optional.empty(), 0, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentParseContext(source, CommandInput.raw(source,
            ""), " ", String.class, "", 0, 0, SuggestionType.ARGUMENT));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentParseContext(source, CommandInput.raw(source,
            ""), "x", String.class, "", 2, 1, SuggestionType.ARGUMENT));
        assertThrows(IllegalArgumentException.class, () -> new ArgumentParseContext(source, CommandInput.raw(source,
            ""), "x", String.class, "", -1, 0, SuggestionType.ARGUMENT));
    }

    @Test
    void commandSourceAndLifecycleDefaultsAreNoops() {
        AtomicReference<String> reply = new AtomicReference<>();
        CommandSource source = new CommandSource() {
            @Override
            public void reply(String message) {
                reply.set(message);
            }
        };
        CommandSource defaultReplySource = new CommandSource() {
        };
        CommandLifecycleListener lifecycle = new CommandLifecycleListener() {
        };

        source.reply(CommandMessage.info("hello"));
        source.reply("world");
        defaultReplySource.reply("ignored");
        lifecycle.commandRegistered(Commands.literal("x").build(), List.of("x"));
        lifecycle.commandUpdated(Commands.literal("x").build(), List.of("x"));
        lifecycle.commandUnregistered(List.of("x"));
        lifecycle.registryRebuilt(List.of());

        assertEquals("world", reply.get());
        assertEquals(Optional.empty(), source.id());
        assertEquals(Optional.empty(), source.name());
        assertEquals(Locale.ROOT, source.locale());
        assertEquals(Optional.empty(), source.unwrap(String.class));
        assertEquals(Optional.empty(), source.metadata("key"));
        assertTrue(source.hasPermission("anything"));
        assertThrows(NullPointerException.class, () -> source.unwrap(null));
        assertThrows(NullPointerException.class, () -> source.metadata(null));
        assertThrows(NullPointerException.class, () -> source.reply((CommandMessage) null));
    }

    @Test
    void registryDefaultsRejectUnsupportedOperationsAndPathValidatesLiteralTokens() {
        RecordingRegistry registry = new RecordingRegistry();
        RecordingCommandBuilder builder = new RecordingCommandBuilder();
        DelegatingRouteBuilder delegatingRoute = new DelegatingRouteBuilder();
        DelegatingCommandBuilder delegatingBuilder = new DelegatingCommandBuilder();

        assertThrows(UnsupportedOperationException.class, registry::caseInsensitiveLiterals);
        assertThrows(UnsupportedOperationException.class, registry::caseInsensitiveOptions);
        assertThrows(UnsupportedOperationException.class, () -> registry.unregister("x"));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").hidden());
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").usage("u"));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").example("e"));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").cooldown(Duration.ofSeconds(1)));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").requirement("r"));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").group("g"));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").suggestAliases(false));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").middleware((context, command,
            path, next) -> next.proceed(context)));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").argumentSuggestions("a", ctx ->
            List.of()));
        assertThrows(UnsupportedOperationException.class, () -> registry.route("x").optionSuggestions("o", ctx ->
            List.of()));
        assertSame(delegatingRoute, delegatingRoute.argumentSuggestions("a", "named", ctx -> List.of()));
        assertSame(delegatingRoute, delegatingRoute.optionSuggestions("o", "named", ctx -> List.of()));

        assertThrows(UnsupportedOperationException.class, builder::hidden);
        assertThrows(UnsupportedOperationException.class, () -> builder.usage("u"));
        assertThrows(UnsupportedOperationException.class, () -> builder.example("e"));
        assertThrows(UnsupportedOperationException.class, () -> builder.cooldown(Duration.ofSeconds(1)));
        assertThrows(UnsupportedOperationException.class, () -> builder.requirement("r"));
        assertThrows(UnsupportedOperationException.class, () -> builder.group("g"));
        assertThrows(UnsupportedOperationException.class, () -> builder.suggestAliases(false));
        assertThrows(UnsupportedOperationException.class, () -> builder.middleware((context, command, path, next) ->
            next.proceed(context)));
        assertThrows(UnsupportedOperationException.class, () -> builder.argumentSuggestions("a", ctx -> List.of()));
        assertThrows(UnsupportedOperationException.class, () -> builder.optionSuggestions("o", ctx -> List.of()));
        assertThrows(UnsupportedOperationException.class, () -> builder.subRoute("child"));
        assertSame(delegatingBuilder, delegatingBuilder.argumentSuggestions("a", "named", ctx -> List.of()));
        assertSame(delegatingBuilder, delegatingBuilder.optionSuggestions("o", "named", ctx -> List.of()));
        assertSame(delegatingBuilder, delegatingBuilder.subRoute("child <target:String>", route -> {
            assertSame(delegatingBuilder.routeBuilder, route);
            route.description("child route");
        }));
        assertEquals("child <target:String>", delegatingBuilder.subRoutePattern);
        assertThrows(NullPointerException.class, () -> delegatingBuilder.subRoute("child", null));

        builder.path("single", child -> child.executes(ctx -> Results.success("ok")));
        builder.path("admin user promote", child -> child.executes(ctx -> Results.success("ok")));
        assertEquals(List.of("single", "admin", "user", "promote"), builder.literals);
        assertThrows(NullPointerException.class, () -> builder.path("x", null));
        assertThrows(NullPointerException.class, () -> builder.path(null, child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path(" ", child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path("x <arg:String>", child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path("x >bad", child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path("x [--flag]", child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path("x --flag]", child -> { }));
        assertThrows(IllegalArgumentException.class, () -> builder.path("x a|b", child -> { }));
    }

    @Test
    void commandGraphSnapshotsRoots() {
        List<CommandNode> roots = new ArrayList<>();
        roots.add(Commands.literal("root").build());
        CommandGraph graph = new CommandGraph(roots);
        roots.clear();

        assertEquals(1, graph.roots().size());
        assertThrows(UnsupportedOperationException.class, () -> graph.roots().add(Commands.literal("x").build()));
        assertThrows(NullPointerException.class, () -> new CommandGraph(null));
    }

    private static final class RecordingRegistry implements CommandRegistry {
        private final RecordingRouteBuilder routeBuilder = new RecordingRouteBuilder();

        @Override
        public void command(String literal, java.util.function.Consumer<CommandBuilder> configure) {
        }

        @Override
        public void register(CommandNode node) {
        }

        @Override
        public RouteBuilder route(String pattern) {
            return routeBuilder;
        }
    }

    private static class RecordingRouteBuilder implements CommandRegistry.RouteBuilder {
        @Override
        public CommandRegistry.RouteBuilder description(String description) {
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder permission(String permission) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
            return new RecordingCommandBuilder();
        }
    }

    private static class RecordingCommandBuilder implements CommandRegistry.CommandBuilder {
        private final List<String> literals = new ArrayList<>();

        @Override
        public CommandRegistry.CommandBuilder description(String description) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder permission(String permission) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder alias(String alias) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder aliases(String... aliases) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder subcommand(
            String literal,
            java.util.function.Consumer<CommandRegistry.CommandBuilder> configure
        ) {
            literals.add(literal);
            configure.accept(this);
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder argument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder greedyArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalGreedyArgument(String name, Class<T> type) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name, String alias) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type) {
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type, String alias) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
            return this;
        }
    }

    private static final class DelegatingRouteBuilder extends RecordingRouteBuilder {
        @Override
        public CommandRegistry.RouteBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder optionSuggestions(String name, SuggestionProvider provider) {
            return this;
        }
    }

    private static final class DelegatingCommandBuilder extends RecordingCommandBuilder {
        private final RecordingRouteBuilder routeBuilder = new RecordingRouteBuilder();
        private String subRoutePattern;

        @Override
        public CommandRegistry.RouteBuilder subRoute(String pattern) {
            subRoutePattern = pattern;
            return routeBuilder;
        }

        @Override
        public CommandRegistry.CommandBuilder argumentSuggestions(String name, SuggestionProvider provider) {
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder optionSuggestions(String name, SuggestionProvider provider) {
            return this;
        }
    }
}
