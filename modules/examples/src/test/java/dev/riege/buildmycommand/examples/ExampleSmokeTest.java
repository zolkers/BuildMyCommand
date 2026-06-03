package dev.riege.buildmycommand.examples;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftRenderedResult;
import dev.riege.buildmycommand.adapters.terminal.TerminalAdapter;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.examples.adapters.BrigadierAdapterExample;
import dev.riege.buildmycommand.examples.adapters.SimpleAdapterExample;
import dev.riege.buildmycommand.examples.adapters.TerminalAdapterExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationGroupExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationMiddlewareExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationParameterExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationRouteExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationRouteMiddlewareExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationRouteSubcommandExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationSubcommandExample;
import dev.riege.buildmycommand.examples.annotations.DeepAnnotationNestingExample;
import dev.riege.buildmycommand.examples.annotations.NestedSubcommandExample;
import dev.riege.buildmycommand.examples.basics.BuilderCommandsExample;
import dev.riege.buildmycommand.examples.basics.BuilderPathExample;
import dev.riege.buildmycommand.examples.basics.DeepSubcommandNestingExample;
import dev.riege.buildmycommand.examples.basics.ManualNodeExample;
import dev.riege.buildmycommand.examples.basics.PlayerManagementBuilderExample;
import dev.riege.buildmycommand.examples.dsl.NestedRouteExample;
import dev.riege.buildmycommand.examples.dsl.RouteDslMiddlewareExample;
import dev.riege.buildmycommand.examples.dsl.RouteDslExample;
import dev.riege.buildmycommand.examples.lifecycle.CooldownExample;
import dev.riege.buildmycommand.examples.lifecycle.MiddlewareAndErrorsExample;
import dev.riege.buildmycommand.examples.minecraft.MinecraftBrigadierExample;
import dev.riege.buildmycommand.examples.minecraft.MinecraftLoaderAdaptersExample;
import dev.riege.buildmycommand.examples.minecraft.MinecraftNativeAdapterExample;
import dev.riege.buildmycommand.examples.permissions.PermissionExample;
import dev.riege.buildmycommand.examples.suggestions.SuggestionExample;
import dev.riege.buildmycommand.examples.testing.TestKitExample;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleSmokeTest {
    @Test
    void basicsAndDslExamplesDispatchSuccessfully() {
        assertSuccess(BuilderCommandsExample.create().dispatch(source(), "p"), "Pong");
        assertSuccess(BuilderCommandsExample.create().dispatch(source(), "echo hello world"), "hello world");
        assertSuccess(BuilderPathExample.dispatch("user rank set promote Ada admin"), "Promoted Ada to admin");
        assertSuccess(BuilderPathExample.dispatch("user rank set demote Ada"), "Demoted Ada");
        assertSuccess(BuilderPathExample.dispatch("user profile open Ada"), "Opening profile for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish permanent ban Ada repeated griefing"
        ), "Permanent ban for Ada: repeated griefing");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "a mod punish temp add Ada spam --duration 15 --silent"
        ), "Temporary punishment added for Ada: spam duration=15 silent=true");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish temporary remove Ada"
        ), "Temporary punishment removed for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish temporary list"
        ), "Temporary punishments for everyone");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish temporary list Ada"
        ), "Temporary punishments for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish permanent unban Ada"
        ), "Permanent ban lifted for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation appeal approve Ada"
        ), "Appeal approved for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation appeal deny Ada no evidence"
        ), "Appeal denied for Ada: no evidence");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation audit player Ada"
        ), "Audit log for Ada");
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation audit staff Bob"
        ), "Staff audit for Bob");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player moderation history Ada"),
            "Moderation history for Ada");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player profile view Ada"), "Profile for Ada");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player profile edit displayname Ada Ada Lovelace"),
            "Renamed Ada to Ada Lovelace");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player inventory clear Ada"),
            "Cleared inventory for Ada");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player moderation warn Ada spam"),
            "Warned Ada: spam");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player moderation mute Ada --minutes 5 spam"),
            "Muted Ada for 5m: spam");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player moderation ban Ada --silent spam"),
            "Banned Ada silent=true: spam");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player inventory give Ada diamond 2 -s"),
            "Gave 2 diamond to Ada silent=true");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player economy balance add Ada 10"),
            "Added 10 coins to Ada");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player economy balance remove Ada 5 -r refund"),
            "Removed 5 coins from Ada: refund");

        assertSuccess(ManualNodeExample.create().dispatch(source("mod.ban"), "ban Ada repeated griefing --silent"),
            "Banned Ada silent=true reason=repeated griefing");
        assertSuccess(RouteDslExample.create().dispatch(source("inventory.give"), "grant Ada diamond -a 2 -s"),
            "Ada gets 2 diamond silent=true");
        assertSuccess(RouteDslMiddlewareExample.dispatch(source("mod.punish", "staff"), "mod punish Ada spam -d 30 -s"),
            "Punished Ada for 30m silent=true: spam [moderation/punish]");
        CommandResult middlewareDenied = RouteDslMiddlewareExample.dispatch(source("mod.punish"), "mod punish Ada spam");
        assertEquals(CommandResult.Status.FAILURE, middlewareDenied.status());
        assertEquals(Optional.of("Missing permission: staff [moderation/punish]"), middlewareDenied.reply());
        assertSuccess(NestedRouteExample.create().dispatch(source("user.rank.set"), "U Role PuT Ada Admin -T"),
            "Ada -> Admin temporary=true");
        assertSuccess(NestedRouteExample.create().dispatch(source("user.rank.clear"), "user rank clear Ada"),
            "Cleared Ada");
    }

    @Test
    void annotationExamplesDispatchSuccessfully() {
        assertSuccess(AnnotationParameterExample.create().dispatch(source(), "kit Ada starter --amount 3 --silent"),
            "Giving 3x starter to Ada silent=true");
        assertEquals(List.of("starter", "builder", "pvp"),
            AnnotationParameterExample.create().suggest(source(), "kit Ada st", 10));
        assertSuccess(AnnotationGroupExample.create().dispatch(source("staff", "admin.reload"), "adm reload"),
            "Reloaded");
        assertSuccess(AnnotationGroupExample.create().dispatch(source("staff"), "admin status"), "OK");
        assertSuccess(AnnotationMiddlewareExample.create().dispatch(source("staff"), "moderation warn Ada spam"),
            "Warned Ada: spam [moderation/warn]");
        assertEquals(CommandResult.Status.FAILURE,
            AnnotationMiddlewareExample.create().dispatch(source(), "moderation warn Ada spam").status());
        assertEquals(Optional.of("Missing permission: staff [moderation/warn]"),
            AnnotationMiddlewareExample.create().dispatch(source(), "moderation warn Ada spam").reply());
        assertSuccess(AnnotationMiddlewareExample.dispatch("moderation status"),
            "Moderation online [moderation/status]");
        assertSuccess(AnnotationRouteExample.create().dispatch(source("mod.punish"), "mod punish Ada spam -d 30 -s"),
            "Punished Ada for 30m silent=true: spam");
        assertSuccess(AnnotationRouteMiddlewareExample.dispatch(
                source("mod.punish", "staff"),
                "mod punish Ada spam -d 30 -s"
            ),
            "Punished Ada for 30m silent=true: spam [moderation/punish]");
        CommandResult annotationRouteMiddlewareDenied = AnnotationRouteMiddlewareExample.dispatch(
            source("mod.punish"),
            "mod punish Ada spam"
        );
        assertEquals(CommandResult.Status.FAILURE, annotationRouteMiddlewareDenied.status());
        assertEquals(Optional.of("Missing permission: staff [moderation/punish]"),
            annotationRouteMiddlewareDenied.reply());
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("u roles put Ada admin -s"),
            "Set Ada to admin silent=true");
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("user note add Ada hello there -p"),
            "private note for Ada: hello there");
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("user note add Ada hello there"),
            "public note for Ada: hello there");
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("user tp Ada"),
            "Teleporting Ada to current world");
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("user teleport Ada nether"),
            "Teleporting Ada to nether");
        assertSuccess(AnnotationSubcommandExample.dispatch("server status"), "Server online");
        assertSuccess(AnnotationSubcommandExample.dispatch("server reload"), "Server reloaded");
        assertSuccess(AnnotationSubcommandExample.dispatch("srv maint"), "Maintenance status");
        assertSuccess(NestedSubcommandExample.dispatch("t m perm g Ada build.fly"),
            "Granted build.fly to Ada");
        assertSuccess(NestedSubcommandExample.dispatch("team member permission revoke Ada build.fly"),
            "Revoked build.fly from Ada");
        assertSuccess(NestedSubcommandExample.dispatch("team member role set Ada admin --priority 10 --temporary"),
            "Set Ada role=admin priority=10 temporary=true");
        assertSuccess(NestedSubcommandExample.dispatch("team member role set Ada helper"),
            "Set Ada role=helper priority=0 temporary=false");
        assertSuccess(DeepAnnotationNestingExample.dispatch("a mod appeal accept Ada"), "Appeal approved for Ada");
        assertSuccess(DeepAnnotationNestingExample.dispatch("a mod punish temp add Ada spam -d 10 -s"),
            "Temporary punishment added for Ada: spam duration=10 silent=true");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation punish temporary remove Ada"),
            "Temporary punishment removed for Ada");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation punish temporary list"),
            "Temporary punishments for everyone");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation punish temporary list Ada"),
            "Temporary punishments for Ada");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation punish permanent ban Ada spam"),
            "Permanent ban for Ada: spam");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation punish permanent unban Ada"),
            "Permanent ban lifted for Ada");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation appeal deny Ada no proof"),
            "Appeal denied for Ada: no proof");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation audit player Ada"),
            "Audit log for Ada");
        assertSuccess(DeepAnnotationNestingExample.dispatch("admin moderation audit staff Bob"),
            "Staff audit for Bob");
    }

    @Test
    void lifecyclePermissionSuggestionAndTestKitExamplesWork() {
        assertSuccess(CooldownExample.create().dispatch(source(), "daily reward"), "Reward claimed");
        ReplyCapturingSource timedSource = new ReplyCapturingSource();
        assertSuccess(MiddlewareAndErrorsExample.create().dispatch(timedSource, "ping"), "Pong");
        assertTrue(timedSource.replies.get(0).startsWith("Command ping took "));
        assertEquals(CommandResult.Status.FAILURE,
            MiddlewareAndErrorsExample.create().dispatch(source(), "explode").status());
        assertEquals(Optional.of("Command failed: boom"),
            MiddlewareAndErrorsExample.create().dispatch(source(), "explode").reply());
        assertEquals(CommandResult.Status.FAILURE, PermissionExample.denied().status());
        assertSuccess(PermissionExample.allowed(), "Reloaded");
        assertSuccess(SuggestionExample.create().dispatch(source(), "message Ada hello there"), "DM Ada");
        assertEquals(CommandResult.Status.FAILURE,
            SuggestionExample.create().dispatch(source(), "message \"\" hello").status());
        assertEquals(List.of("Ada", "Alex"), SuggestionExample.suggestTargets("message A", 9));
        assertSuccess(TestKitExample.exerciseCommand(), "Banned Ada");
    }

    @Test
    void adapterExamplesExecuteThroughTheirAdapters() throws Exception {
        CommandAdapter<
            SimpleAdapterExample.ChatUser,
            SimpleAdapterExample.ChatMessage,
            SimpleAdapterExample.ChatReply
            > chat = SimpleAdapterExample.create();
        SimpleAdapterExample.ChatReply reply = chat.execute(
            new SimpleAdapterExample.ChatUser("Ada", Set.of()),
            new SimpleAdapterExample.ChatMessage("!hello")
        );
        assertEquals(new SimpleAdapterExample.ChatReply(true, "Hello Ada"), reply);
        assertEquals(new SimpleAdapterExample.ChatReply(true, "Hello Ada"), chat.execute(
            new SimpleAdapterExample.ChatUser("Ada", Set.of()),
            new SimpleAdapterExample.ChatMessage("hello")
        ));
        assertEquals(false, chat.mapSource(new SimpleAdapterExample.ChatUser("Ada", Set.of())).hasPermission("x"));
        assertEquals(new SimpleAdapterExample.ChatReply(false, "Unknown command: missing"), chat.execute(
            new SimpleAdapterExample.ChatUser("Ada", Set.of()),
            new SimpleAdapterExample.ChatMessage("!missing")
        ));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TerminalAdapter terminal = TerminalAdapterExample.attach(
            new ByteArrayInputStream("ping\n".getBytes(StandardCharsets.UTF_8)),
            new PrintStream(output, true, StandardCharsets.UTF_8)
        );
        terminal.runOnce(source());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Pong"));

        CommandDispatcher<BrigadierAdapterExample.NativeSource> dispatcher = BrigadierAdapterExample.dispatcher();
        assertEquals(1, dispatcher.execute(
            "adm ban Ada --silent",
            new BrigadierAdapterExample.NativeSource(Set.of("admin.ban"))
        ));

        MinecraftRenderedResult nativeResult = MinecraftNativeAdapterExample.execute();
        assertEquals(1, nativeResult.numericResult());
        assertEquals(Optional.of("Home set: base"), nativeResult.message());

        assertEquals(1, MinecraftBrigadierExample.createForFabricForgeNeoForgeStyleDispatchers()
            .execute(
                new MinecraftBrigadierExample.FakeStack(Set.of("minecraft.command.worldborder")),
                "/wb set 10"
            ));

        CommandDispatcher<MinecraftLoaderAdaptersExample.NativeCommandSource> fabricLegacy =
            new CommandDispatcher<>();
        CommandDispatcher<MinecraftLoaderAdaptersExample.NativeCommandSource> fabricModern =
            new CommandDispatcher<>();
        CommandDispatcher<MinecraftLoaderAdaptersExample.NativeCommandSource> forgeLegacy =
            new CommandDispatcher<>();
        CommandDispatcher<MinecraftLoaderAdaptersExample.NativeCommandSource> forgeModern =
            new CommandDispatcher<>();
        CommandDispatcher<MinecraftLoaderAdaptersExample.NativeCommandSource> neoForge =
            new CommandDispatcher<>();
        MinecraftLoaderAdaptersExample.NativeCommandSource loaderSource =
            new MinecraftLoaderAdaptersExample.NativeCommandSource("Ada", true);
        MinecraftLoaderAdaptersExample.NativeCommandSource guestSource =
            new MinecraftLoaderAdaptersExample.NativeCommandSource("Guest", false);

        MinecraftLoaderAdaptersExample.registerFabric1165(fabricLegacy);
        MinecraftLoaderAdaptersExample.registerFabricModern(fabricModern);
        MinecraftLoaderAdaptersExample.registerForge1165(forgeLegacy);
        MinecraftLoaderAdaptersExample.registerForgeModern(forgeModern);
        MinecraftLoaderAdaptersExample.registerNeoForge(neoForge);

        assertEquals("Ada", loaderSource.name());
        assertTrue(guestSource.hasPermission(""));
        assertEquals(false, guestSource.hasPermission("mod.reload"));
        assertEquals(Optional.of("Ada"),
            MinecraftLoaderAdaptersExample.fabricModernRegistration().bridge().mapSource(loaderSource).name());
        assertEquals(1, fabricLegacy.execute("mod reload config", loaderSource));
        assertEquals(1, fabricModern.execute("moderation reload cache", loaderSource));
        assertEquals(1, forgeLegacy.execute("mod reload users", loaderSource));
        assertEquals(1, forgeModern.execute("moderation reload permissions", loaderSource));
        assertEquals(1, neoForge.execute("moderation reload permissions", loaderSource));
    }

    private static void assertSuccess(CommandResult result, String reply) {
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(Optional.of(reply), result.reply());
    }

    private static CommandSource source(String... permissions) {
        return new CommandSource() {
            @Override
            public Optional<String> name() {
                return Optional.of("Ada");
            }

            @Override
            public boolean hasPermission(String permission) {
                return List.of(permissions).contains(permission);
            }
        };
    }

    private static final class ReplyCapturingSource implements CommandSource {
        private final List<String> replies = new java.util.ArrayList<>();

        @Override
        public void reply(String message) {
            replies.add(message);
        }
    }
}
