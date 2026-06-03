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
import dev.riege.buildmycommand.examples.annotations.AnnotationParameterExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationRouteExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationRouteSubcommandExample;
import dev.riege.buildmycommand.examples.annotations.AnnotationSubcommandExample;
import dev.riege.buildmycommand.examples.annotations.DeepAnnotationNestingExample;
import dev.riege.buildmycommand.examples.basics.BuilderCommandsExample;
import dev.riege.buildmycommand.examples.basics.BuilderPathExample;
import dev.riege.buildmycommand.examples.basics.DeepSubcommandNestingExample;
import dev.riege.buildmycommand.examples.basics.ManualNodeExample;
import dev.riege.buildmycommand.examples.basics.PlayerManagementBuilderExample;
import dev.riege.buildmycommand.examples.dsl.NestedRouteExample;
import dev.riege.buildmycommand.examples.dsl.RouteDslExample;
import dev.riege.buildmycommand.examples.lifecycle.CooldownExample;
import dev.riege.buildmycommand.examples.lifecycle.MiddlewareAndErrorsExample;
import dev.riege.buildmycommand.examples.minecraft.MinecraftBrigadierExample;
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
        assertSuccess(DeepSubcommandNestingExample.dispatch(
            "admin moderation punish permanent ban Ada repeated griefing"
        ), "Permanent ban for Ada: repeated griefing");
        assertSuccess(PlayerManagementBuilderExample.dispatch("player moderation history Ada"),
            "Moderation history for Ada");

        assertSuccess(ManualNodeExample.create().dispatch(source("mod.ban"), "ban Ada repeated griefing --silent"),
            "Banned Ada silent=true reason=repeated griefing");
        assertSuccess(RouteDslExample.create().dispatch(source("inventory.give"), "grant Ada diamond -a 2 -s"),
            "Ada gets 2 diamond silent=true");
        assertSuccess(NestedRouteExample.create().dispatch(source("user.rank.set"), "U Role PuT Ada Admin -T"),
            "Ada -> Admin temporary=true");
    }

    @Test
    void annotationExamplesDispatchSuccessfully() {
        assertSuccess(AnnotationParameterExample.create().dispatch(source(), "kit Ada starter --amount 3 --silent"),
            "Giving 3x starter to Ada silent=true");
        assertSuccess(AnnotationGroupExample.create().dispatch(source("staff", "admin.reload"), "adm reload"),
            "Reloaded");
        assertSuccess(AnnotationRouteExample.create().dispatch(source("mod.punish"), "mod punish Ada spam -d 30 -s"),
            "Punished Ada for 30m silent=true: spam");
        assertSuccess(AnnotationRouteSubcommandExample.dispatch("u roles put Ada admin -s"),
            "Set Ada to admin silent=true");
        assertSuccess(AnnotationSubcommandExample.dispatch("server status"), "Server online");
        assertSuccess(DeepAnnotationNestingExample.dispatch("a mod appeal accept Ada"), "Appeal approved for Ada");
    }

    @Test
    void lifecyclePermissionSuggestionAndTestKitExamplesWork() {
        assertSuccess(CooldownExample.create().dispatch(source(), "daily reward"), "Reward claimed");
        assertEquals(CommandResult.Status.FAILURE,
            MiddlewareAndErrorsExample.create().dispatch(source(), "explode").status());
        assertEquals(Optional.of("Command failed: boom"),
            MiddlewareAndErrorsExample.create().dispatch(source(), "explode").reply());
        assertEquals(CommandResult.Status.FAILURE, PermissionExample.denied().status());
        assertSuccess(PermissionExample.allowed(), "Reloaded");
        assertSuccess(SuggestionExample.create().dispatch(source(), "message Ada hello there"), "DM Ada");
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
}
