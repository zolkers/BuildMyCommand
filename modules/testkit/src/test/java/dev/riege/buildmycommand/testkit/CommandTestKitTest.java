package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTestKitTest {
    @Test
    void assertsSuccessfulDispatch() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        CommandTestKit kit = CommandTestKit.create(framework);

        assertDoesNotThrow(() -> kit.dispatch("ping")
            .assertSuccess()
            .assertMessageContains("Pong"));
    }

    @Test
    void fluentDispatchAssertionsCheckExactReplies() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("ban", command -> command
                .permission("cmd.ban")
                .executes(ctx -> Results.success("banned")));
        CommandTestKit allowed = CommandTestKit.create(
            framework,
            TestCommandSource.builder().permission("cmd.ban").build()
        );
        CommandTestKit denied = CommandTestKit.create(framework, TestCommandSource.create());

        allowed.assertDispatch("ban").succeedsWith("banned");
        denied.assertDispatch("ban").failsWith("Missing permission: cmd.ban");
    }

    @Test
    void assertsSuggestionsAndSchema() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("ban", command -> command
                .description("Ban a user")
                .executes(ctx -> Results.silent()));
        CommandTestKit kit = CommandTestKit.create(framework);

        kit.assertSuggestions("b").contains("ban");
        kit.assertSchema().containsCommand("ban").contains("description Ban a user");
    }

    @Test
    void fakeSourceExposesPermissionsLocaleAndMetadata() {
        TestCommandSource source = TestCommandSource.builder()
            .id("42")
            .name("Ada")
            .locale(Locale.FRANCE)
            .permission("cmd.use")
            .metadata("kind", "tester")
            .build();

        assertEquals("42", source.id().orElseThrow());
        assertEquals("Ada", source.name().orElseThrow());
        assertEquals(Locale.FRANCE, source.locale());
        assertEquals("tester", source.metadata("kind").orElseThrow());
        assertEquals(true, source.hasPermission("cmd.use"));
    }

    @Test
    void fakePlatformIsUsedByDispatchInput() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("platform", command -> command.executes(ctx -> Results.success(
            ctx.commandInput().platform().id()
        )));
        CommandTestKit kit = CommandTestKit.create(framework)
            .platform(TestPlatforms.fake("custom"));

        kit.assertDispatch("platform").succeedsWith("custom");
    }
}
