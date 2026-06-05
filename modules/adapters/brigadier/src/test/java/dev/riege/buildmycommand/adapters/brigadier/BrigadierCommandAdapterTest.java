/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.SuggestAliases;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrigadierCommandAdapterTest {
    @Test
    void buildsBrigadierTreeFromFrameworkGraphAndDispatchesThroughCore() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);

        framework.registry()
            .route("user rank set <target:String> [--silent|-s]")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class) + ":" + ctx.flag("silent"));
                return Results.success("ok");
            });

        LiteralCommandNode<NativeSource> root = bridge.roots().get(0);
        LiteralCommandNode<NativeSource> set = literal(literal(root, "rank"), "set");
        ArgumentCommandNode<NativeSource, ?> target =
            assertInstanceOf(ArgumentCommandNode.class, set.getChild("target"));

        int result = target.getCommand().run(brigadierContext(new NativeSource(), "user rank set Ada --silent"));

        assertEquals("user", root.getName());
        assertNotNull(target.getCustomSuggestions());
        assertTrue(target.getChild("_bmc_input").getName().startsWith("_bmc_input"));
        assertEquals(1, result);
        assertEquals("Ada:true", executed.get());
    }

    @Test
    void simpleArgumentLeavesDoNotExposeFrameworkTunnelAsNextSuggestion() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("wecc bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.success("bang " + ctx.arg("target", String.class)));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        LiteralCommandNode<NativeSource> root = literal(bridge.roots().get(0), "bang");
        ArgumentCommandNode<NativeSource, ?> target =
            assertInstanceOf(ArgumentCommandNode.class, root.getChild("target"));
        assertNull(target.getChild("_bmc_input"));
        assertFalse(suggestions(dispatcher, "wecc bang Ada ").contains("_bmc_input"));
    }

    @Test
    void registersRootAliasAsRedirectNode() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);

        framework.registry().command("ping", command -> command
            .alias("p")
            .executes(ctx -> Results.success("pong")));

        LiteralCommandNode<NativeSource> alias = bridge.roots().get(1);

        assertEquals("p", alias.getName());
        assertEquals("ping", alias.getRedirect().getName());
    }

    @Test
    void dispatcherExecutesNestedLiteralAliasesThroughFramework() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("user rank|roles set|put <target:String>")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class));
                return Results.success("ok");
            });
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, dispatcher.execute("user roles put Ada", new NativeSource()));
        assertEquals("Ada", executed.get());
    }

    @Test
    void dispatcherDoesNotLetFallbackTunnelEatIncompleteOrUnknownSubcommands() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("wecc ping")
            .executes(ctx -> Results.success("pong"));
        framework.registry()
            .route("wecc bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.success("bang " + ctx.arg("target", String.class)));
        framework.registry()
            .route("wecc quiet|q")
            .suggestAliases(false)
            .executes(ctx -> Results.success("quiet"));
        framework.registry()
            .route("wecc say|s <message:String...>")
            .suggestAliases(false)
            .executes(ctx -> Results.success(ctx.arg("message", String.class)));
        framework.registry()
            .route("wecc maybe|m [target:String]")
            .suggestAliases(false)
            .executes(ctx -> Results.success(ctx.optionalArg("target", String.class).orElse("none")));
        framework.registry().command("wecc", command -> command
            .subcommand("group", group -> group
                .alias("g")
                .suggestAliases(false)
                .subcommand("reload", reload -> reload.executes(ctx -> Results.success("reloaded")))));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, dispatcher.execute("wecc ping", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc bang Ada", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc b Ada", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc B Ada", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc q", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc s hello", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc m", new NativeSource()));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("wecc bang", new NativeSource()));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("wecc b", new NativeSource()));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("wecc s", new NativeSource()));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("wecc g", new NativeSource()));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("wecc nope", new NativeSource()));
        assertFalse(dispatcher.parse("wecc nope", new NativeSource()).getExceptions().isEmpty());
        assertFalse(dispatcher.parse("wecc b", new NativeSource()).getExceptions().isEmpty());
        assertTrue(suggestions(dispatcher, "wecc ").contains("bang"));
        assertTrue(suggestions(dispatcher, "wecc ").contains("ping"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("b"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("q"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("s"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("m"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("g"));
        assertEquals(List.of("bang"), suggestions(dispatcher, "wecc b"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("_bmc_input"));
        assertFalse(suggestions(dispatcher, "wecc nope").contains("_bmc_input"));
    }

    @Test
    void dispatcherHidesSuppressedSubRouteAliasesFromAnnotatedCommands() throws Exception {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new AnnotatedWeccCommands());
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, dispatcher.execute("wecc bang Ada", new NativeSource()));
        assertEquals(1, dispatcher.execute("wecc b Ada", new NativeSource()));
        assertEquals(List.of("bang"), suggestions(dispatcher, "wecc "));
        assertEquals(List.of("bang"), suggestions(dispatcher, "wecc b"));
        assertFalse(suggestions(dispatcher, "wecc ").contains("b"));
        assertFalse(suggestions(dispatcher, "wecc b").contains("b"));
    }

    @Test
    void fallbackTunnelRejectsOnlyInputsThatShouldBeOwnedByBrigadierChildren() throws Exception {
        CommandFramework strictFramework = CommandFramework.create();
        strictFramework.registry()
            .route("wecc ping")
            .executes(ctx -> Results.success("pong"));
        strictFramework.registry()
            .route("wecc bang|b <target:String>")
            .executes(ctx -> Results.success("bang " + ctx.arg("target", String.class)));
        strictFramework.registry()
            .route("wecc quiet|q")
            .executes(ctx -> Results.success("quiet"));
        BrigadierCommandAdapter<NativeSource> strictBridge =
            BrigadierCommandAdapter.create(strictFramework, NativeSource::source);
        dev.riege.buildmycommand.api.CommandNode strictRoot = strictFramework.graph().roots().get(0);
        dev.riege.buildmycommand.api.CommandNode leaf = strictRoot.children().stream()
            .filter(child -> child.literal().equals("ping"))
            .findFirst()
            .orElseThrow();

        Method shouldRejectTunnel = BrigadierCommandAdapter.class.getDeclaredMethod(
            "shouldRejectTunnel",
            dev.riege.buildmycommand.api.CommandNode.class,
            String.class
        );
        shouldRejectTunnel.setAccessible(true);

        assertFalse((boolean) shouldRejectTunnel.invoke(strictBridge, leaf, "anything"));
        assertFalse((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, " "));
        assertFalse((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, "--silent"));
        assertFalse((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, "b Ada"));
        assertFalse((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, "q"));
        assertTrue((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, "bang"));
        assertTrue((boolean) shouldRejectTunnel.invoke(strictBridge, strictRoot, "BANG"));

        CommandFramework caseInsensitiveFramework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();
        caseInsensitiveFramework.registry()
            .route("wecc bang|b <target:String>")
            .executes(ctx -> Results.success("bang " + ctx.arg("target", String.class)));
        BrigadierCommandAdapter<NativeSource> caseInsensitiveBridge =
            BrigadierCommandAdapter.create(caseInsensitiveFramework, NativeSource::source);

        assertFalse((boolean) shouldRejectTunnel.invoke(
            caseInsensitiveBridge,
            caseInsensitiveFramework.graph().roots().get(0),
            "BANG"
        ));

        Method hasRemainderAfterFirstToken = BrigadierCommandAdapter.class.getDeclaredMethod(
            "hasRemainderAfterFirstToken",
            String.class
        );
        hasRemainderAfterFirstToken.setAccessible(true);
        assertFalse((boolean) hasRemainderAfterFirstToken.invoke(null, "bang "));
        assertTrue((boolean) hasRemainderAfterFirstToken.invoke(null, "bang Ada"));
    }

    @Test
    void exposesGenericAdapterSdkForRawBrigadierInput() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("pong")));

        CommandAdapter<NativeSource, String, Integer> adapter = bridge;
        CommandInput input = adapter.mapInput(new NativeSource(), "/ping");

        assertEquals("brigadier", adapter.config().adapterId());
        assertEquals("brigadier", adapter.runtime().platform().id());
        assertEquals("/ping", input.rawInput());
        assertEquals("ping", input.normalizedInput());
        assertEquals(1, adapter.execute(new NativeSource(), "/ping"));
    }

    @Test
    void rejectsNullMappedSource() {
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(
            CommandFramework.create(),
            source -> null
        );

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> bridge.mapSource(new NativeSource()));

        assertEquals("mapped source", exception.getMessage());
    }

    @Test
    void acceptsCustomPlatformAndAdapterConfig() {
        CommandPlatform platform = new CommandPlatform("minecraft", "Minecraft", false, true, true);
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(
            CommandFramework.create(),
            NativeSource::source,
            platform,
            new AdapterConfig("minecraft-brigadier", "Minecraft Brigadier", AdapterCapabilities.from(platform))
        );

        assertEquals("minecraft-brigadier", bridge.config().adapterId());
        assertEquals("minecraft", bridge.runtime().platform().id());
    }

    @Test
    void registersProjectedRootsIntoAnyBrigadierDispatcher() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.alias("p").executes(ctx -> Results.success("pong")));

        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        assertEquals(Set.of("ping", "p"), bridge.registration().register(dispatcher));
        assertEquals(bridge, bridge.registration().adapter());
        assertEquals(List.of("ping", "p"), bridge.registration().labels());
        assertEquals(List.of("ping", "p"), bridge.registration().projectedRoots().get(0).registrationLabels());
        assertEquals(List.of("ping"), bridge.rootLiterals());
        assertEquals(false, bridge.caseInsensitiveLiterals());
        assertNotNull(dispatcher.getRoot().getChild("ping"));
        assertNotNull(dispatcher.getRoot().getChild("p"));
        assertEquals("ping", dispatcher.getRoot().getChild("p").getRedirect().getName());
    }

    @Test
    void registrationDoesNotOverrideExistingDispatcherRoots() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("bmc")));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<NativeSource>literal("ping")
            .executes(context -> 42));

        assertEquals(Set.of(), bridge.registration().register(dispatcher));

        assertEquals(42, dispatcher.execute("ping", new NativeSource()));
    }

    @Test
    void dispatcherHonorsFrameworkCaseInsensitiveLiteralsUnderRegisteredRootsOnly() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        AtomicReference<String> executed = new AtomicReference<>();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("admin ban <target:String> [--silent|-s]")
            .executes(ctx -> {
                executed.set(ctx.arg("target", String.class) + ":" + ctx.flag("silent"));
                return Results.success("ok");
            });
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("ADMIN ban Ada --SILENT", new NativeSource()));
        assertEquals(1, dispatcher.execute("admin BAN Ada -S", new NativeSource()));
        assertEquals("Ada:true", executed.get());
    }

    @Test
    void dispatcherFallbackTunnelDelegatesSuggestionsUnderRegisteredRootsOnly() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .build();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("admin ban <target:String>")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertEquals(List.of(), suggestions(dispatcher, "unknown"));
        assertEquals(List.of("admin"), suggestions(dispatcher, "A"));
        assertEquals(List.of("ban"), suggestions(dispatcher, "admin B"));
    }

    @Test
    void argumentNodesDelegateSuggestionsWithTooltipsToFramework() throws Exception {
        CommandFramework framework = CommandFramework.builder()
            .argumentParser(Target.class, new dev.riege.buildmycommand.api.ArgumentParser<>() {
                @Override
                public ArgumentParseResult<Target> parse(String rawToken, dev.riege.buildmycommand.api.ArgumentParseContext context) {
                    return ArgumentParseResult.success(new Target(rawToken));
                }

                @Override
                public List<Suggestion> suggestions(dev.riege.buildmycommand.api.ArgumentParseContext context) {
                    return List.of(new Suggestion(
                        "Ada",
                        Optional.of("Online"),
                        context.replacementStart(),
                        context.replacementEnd(),
                        SuggestionType.ARGUMENT,
                        0
                    ));
                }
            })
            .build();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("message", message -> message
            .argument("target", Target.class)
            .executes(ctx -> Results.success("ok")));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        com.mojang.brigadier.suggestion.Suggestion suggestion =
            dispatcher.getCompletionSuggestions(dispatcher.parse("message A", new NativeSource()))
                .get()
                .getList()
                .get(0);
        assertEquals("Ada", suggestion.getText());
        assertEquals("Online", suggestion.getTooltip().getString());
    }

    @Test
    void frameworkTunnelSuggestionsPreserveReplacementRangeForTrailingOptions() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]")
            .executes(ctx -> Results.success("help"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        String input = "help --page 2 --alph";
        com.mojang.brigadier.suggestion.Suggestion suggestion =
            dispatcher.getCompletionSuggestions(dispatcher.parse(input, new NativeSource()))
                .get()
                .getList()
                .get(0);
        assertEquals("--alphabetic", suggestion.getText());
        assertEquals(StringRange.between(input.indexOf("--alph"), input.length()), suggestion.getRange());
        assertEquals("help --page 2 --alphabetic", suggestion.apply(input));
    }

    @Test
    void frameworkTunnelSuggestionsPreserveSeparatorForSlashPrefixedInputs() throws Exception {
        SuggestionsBuilder builder = new SuggestionsBuilder("/help deb", "/help ".length());
        Suggestion frameworkSuggestion = new Suggestion(
            "debug",
            Optional.empty(),
            "help ".length(),
            "help deb".length(),
            SuggestionType.ARGUMENT,
            0
        );
        Method suggest = BrigadierCommandAdapter.class.getDeclaredMethod(
            "suggest",
            SuggestionsBuilder.class,
            Suggestion.class
        );
        suggest.setAccessible(true);

        suggest.invoke(null, builder, frameworkSuggestion);

        com.mojang.brigadier.suggestion.Suggestion suggestion = builder.build().getList().get(0);
        assertEquals("debug", suggestion.getText());
        assertEquals(StringRange.between("/help ".length(), "/help deb".length()), suggestion.getRange());
        assertEquals("/help debug", suggestion.apply("/help deb"));

        SuggestionsBuilder plainBuilder = new SuggestionsBuilder("help deb", "help ".length());
        suggest.invoke(null, plainBuilder, frameworkSuggestion);
        com.mojang.brigadier.suggestion.Suggestion plainSuggestion = plainBuilder.build().getList().get(0);
        assertEquals(StringRange.between("help ".length(), "help deb".length()), plainSuggestion.getRange());
        assertEquals("help debug", plainSuggestion.apply("help deb"));
    }

    @Test
    void greedyArgumentsUseGreedyBrigadierArgumentType() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        AtomicReference<String> executed = new AtomicReference<>();
        framework.registry().command("say", say -> say
            .greedyArgument("message", String.class)
            .executes(ctx -> {
                executed.set(ctx.arg("message", String.class));
                return Results.success("ok");
            }));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, assertDoesNotThrow(() -> dispatcher.execute("say hello world", new NativeSource())));
        assertEquals("hello world", executed.get());
    }

    @Test
    void buildsRecursiveArgumentBranchesAndOptionalGreedyArguments() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        AtomicReference<String> executed = new AtomicReference<>();
        framework.registry().command("pair", pair -> pair
            .argument("left", String.class)
            .argument("right", String.class)
            .executes(ctx -> {
                executed.set(ctx.arg("left", String.class) + ":" + ctx.arg("right", String.class));
                return Results.success("ok");
            }));
        framework.registry().command("note", note -> note
            .optionalGreedyArgument("message", String.class)
            .executes(ctx -> Results.success(ctx.argOr("message", "empty"))));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(1, assertDoesNotThrow(() -> dispatcher.execute("pair a b", new NativeSource())));
        assertEquals("a:b", executed.get());
        assertEquals(1, assertDoesNotThrow(() -> dispatcher.execute("note hello world", new NativeSource())));
    }

    @Test
    void dispatcherSuggestionsIncludeAliasesAndShortOptions() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("give|grant <target:String> [--silent|-s] [--amount:Integer|-a]")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(List.of("give", "grant"), suggestions(dispatcher, "g"));
        assertEquals(Set.of("--silent", "-s", "--amount", "-a"),
            Set.copyOf(suggestions(dispatcher, "give Ada -")));
    }

    @Test
    void dispatcherKeepsUnknownRootsAsInvalidInput() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("pong")));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertNull(dispatcher.getRoot().getChild("_bmc_input"));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("PING", new NativeSource()));
        assertEquals(List.of("ping"), suggestions(dispatcher, "PING"));
        assertFalse(suggestions(dispatcher, "nope").contains("_bmc_input"));
    }

    @Test
    void dispatcherDelegatesDeniedInvalidArgumentInputToFrameworkPermissionCheck() throws Exception {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry()
            .route("user rank set <level:Integer>")
            .permission("rank.set")
            .executes(ctx -> Results.success("ok"));
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();

        bridge.registration().register(dispatcher);

        assertEquals(0, dispatcher.execute("user rank set nope", new NativeSource(false)));
    }

    @Test
    void renderMapsOnlySuccessToBrigadierSuccessCode() {
        CommandFramework framework = CommandFramework.create();
        BrigadierCommandAdapter<NativeSource> bridge = BrigadierCommandAdapter.create(framework, NativeSource::source);
        framework.registry().command("quiet", command -> command.executes(ctx -> Results.silent()));

        assertEquals(0, bridge.execute(new NativeSource(), "missing"));
        assertEquals(0, bridge.execute(new NativeSource(), "quiet"));
    }

    @Test
    void rejectsNullConstructionAndRegistrationInputs() {
        CommandFramework framework = CommandFramework.create();
        CommandPlatform platform = new CommandPlatform("brigadier", "Brigadier", false, true, true);
        AdapterConfig config = new AdapterConfig("brigadier", "Brigadier", AdapterCapabilities.from(platform));

        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(null, NativeSource::source));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, null));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, NativeSource::source,
            null, config));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, NativeSource::source,
            platform, null));
        assertThrows(NullPointerException.class, () -> new BrigadierRegistration<NativeSource>(null));
        assertThrows(NullPointerException.class, () -> new BrigadierRoot<NativeSource>(null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new BrigadierRoot<>(
            LiteralArgumentBuilder.<NativeSource>literal("root").build(),
            null,
            List.of()
        ));
        assertThrows(NullPointerException.class, () -> new BrigadierRoot<>(
            LiteralArgumentBuilder.<NativeSource>literal("root").build(),
            List.of(),
            null
        ));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, NativeSource::source)
            .registration()
            .register(null));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, source -> null)
            .mapSource(new NativeSource()));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, NativeSource::source)
            .mapSource(null));
        assertThrows(NullPointerException.class, () -> BrigadierCommandAdapter.create(framework, NativeSource::source)
            .mapInput(new NativeSource(), null));
    }

    private static LiteralCommandNode<NativeSource> literal(LiteralCommandNode<NativeSource> parent, String name) {
        return assertInstanceOf(LiteralCommandNode.class, parent.getChild(name));
    }

    private static List<String> suggestions(CommandDispatcher<NativeSource> dispatcher, String input) throws Exception {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new NativeSource()))
            .get()
            .getList()
            .stream()
            .map(com.mojang.brigadier.suggestion.Suggestion::getText)
            .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CommandContext<NativeSource> brigadierContext(NativeSource source, String input) {
        return new CommandContext(source, input, Map.of(), null, null, List.of(),
            StringRange.at(0), null, null, false);
    }

    private record NativeSource(boolean allowed) {
        private NativeSource() {
            this(true);
        }

        CommandSource source() {
            return new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return allowed;
                }
            };
        }
    }

    @Command("wecc")
    @CaseInsensitive
    static final class AnnotatedWeccCommands {
        @SuggestAliases(false)
        @SubRoute("bang|b <target:String>")
        CommandResult bang(@RouteCtx dev.riege.buildmycommand.api.CommandContext context) {
            return Results.success("bang " + context.arg("target", String.class));
        }
    }

    private record Target(String name) {
    }
}
