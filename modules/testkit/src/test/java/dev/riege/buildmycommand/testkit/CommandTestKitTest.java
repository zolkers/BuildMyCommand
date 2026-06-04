/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.MessageLevel;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            .source(TestCommandSource.builder().name("Ada").build())
            .platform(TestPlatforms.fake("custom"));

        kit.assertDispatch("platform").succeedsWith("custom");
    }

    @Test
    void dispatchAssertionsReportMismatchedStatusesAndReplies() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ok", command -> command.executes(ctx -> Results.success("done")));
        framework.registry().command("silent", command -> command.executes(ctx -> Results.silent()));
        CommandTestKit kit = CommandTestKit.create(framework);

        assertEquals("Expected failure but was SUCCESS",
            assertThrows(AssertionError.class, () -> kit.dispatch("ok").assertFailure()).getMessage());
        assertEquals("Expected success but was SILENT",
            assertThrows(AssertionError.class, () -> kit.dispatch("silent").assertSuccess()).getMessage());
        assertEquals("Expected reply 'other' but was 'done'",
            assertThrows(AssertionError.class, () -> kit.dispatch("ok").assertReply("other")).getMessage());
        assertEquals("Expected reply 'missing' but reply was empty",
            assertThrows(AssertionError.class, () -> kit.dispatch("silent").assertReply("missing")).getMessage());
        assertEquals("Expected reply to contain 'x' but was 'done'",
            assertThrows(AssertionError.class, () -> kit.dispatch("ok").assertMessageContains("x")).getMessage());
        assertEquals("Expected reply to contain 'x' but reply was empty",
            assertThrows(AssertionError.class, () -> kit.dispatch("silent").assertMessageContains("x")).getMessage());
        assertThrows(NullPointerException.class, () -> kit.dispatch(null));
    }

    @Test
    void suggestionAndSchemaAssertionsReportMissingOrUnexpectedValues() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ban", command -> command.executes(ctx -> Results.silent()));
        CommandTestKit kit = CommandTestKit.create(framework);

        assertEquals("Expected suggestions to contain 'kick' but were [ban]",
            assertThrows(AssertionError.class, () -> kit.assertSuggestions("b").contains("kick")).getMessage());
        assertEquals("Expected suggestions not to contain 'ban' but were [ban]",
            assertThrows(AssertionError.class, () -> kit.assertSuggestions("b").doesNotContain("ban")).getMessage());
        assertEquals("Expected schema to contain 'command kick' but was:\ncommand ban",
            assertThrows(AssertionError.class, () -> kit.assertSchema().containsCommand("kick")).getMessage());
        assertEquals("Expected schema to contain 'permission x' but was:\ncommand ban",
            assertThrows(AssertionError.class, () -> kit.assertSchema().contains("permission x")).getMessage());
        assertEquals("command ban", kit.assertSchema().value());
        assertEquals(java.util.List.of("ban"), kit.assertSuggestions("b").values());
        kit.assertSuggestions("b").doesNotContain("kick");
        assertThrows(NullPointerException.class, () -> kit.assertSuggestions(null));
        assertThrows(NullPointerException.class, () -> kit.assertSuggestions("b").contains(null));
        assertThrows(NullPointerException.class, () -> kit.assertSuggestions("b").doesNotContain(null));
        assertThrows(NullPointerException.class, () -> kit.assertSchema().containsCommand(null));
        assertThrows(NullPointerException.class, () -> kit.assertSchema().contains(null));
    }

    @Test
    void sourceAndPlatformBuildersValidateAndSnapshotData() {
        TestCommandSource anonymous = TestCommandSource.builder()
            .id(null)
            .name(null)
            .permissions("a", "b")
            .metadata("level", 4)
            .build();

        anonymous.reply(new CommandMessage("hello", MessageLevel.INFO, Map.of()));

        assertEquals(Optional.empty(), anonymous.id());
        assertEquals(Optional.empty(), anonymous.name());
        assertEquals(true, anonymous.hasPermission("a"));
        assertEquals(Set.of("a", "b"), anonymous.permissions());
        assertEquals(false, anonymous.hasPermission("missing"));
        assertEquals(4, anonymous.metadata("level").orElseThrow());
        assertEquals(java.util.List.of(CommandMessage.info("hello")), anonymous.replies());
        assertEquals("Fake game", TestPlatforms.fake("game").displayName());
        assertEquals(false, TestPlatforms.fake("game", false, false, false).supportsAutocomplete());
        assertThrows(UnsupportedOperationException.class, () -> anonymous.replies().add(CommandMessage.info("x")));
        assertThrows(NullPointerException.class, () -> anonymous.metadata(null));
        assertThrows(NullPointerException.class, () -> anonymous.reply((CommandMessage) null));
        assertThrows(NullPointerException.class, () -> anonymous.hasPermission(null));
        assertThrows(IllegalArgumentException.class, () -> TestCommandSource.builder().id(" "));
        assertThrows(IllegalArgumentException.class, () -> TestCommandSource.builder().name(""));
        assertThrows(NullPointerException.class, () -> TestCommandSource.builder().locale(null));
        assertThrows(IllegalArgumentException.class, () -> TestCommandSource.builder().permission(" "));
        assertThrows(NullPointerException.class, () -> TestCommandSource.builder().permissions((String[]) null));
        assertThrows(NullPointerException.class, () -> TestCommandSource.builder().metadata("key", null));
        assertThrows(IllegalArgumentException.class, () -> TestCommandSource.builder().metadata(" ", "value"));
    }

    @Test
    void testKitRejectsNullConfiguration() {
        CommandFramework framework = CommandFramework.create();
        CommandTestKit kit = CommandTestKit.create(framework);

        assertThrows(NullPointerException.class, () -> CommandTestKit.create(null));
        assertThrows(NullPointerException.class, () -> CommandTestKit.create(framework, null));
        assertThrows(NullPointerException.class, () -> kit.source(null));
        assertThrows(NullPointerException.class, () -> kit.platform(null));
    }
}
