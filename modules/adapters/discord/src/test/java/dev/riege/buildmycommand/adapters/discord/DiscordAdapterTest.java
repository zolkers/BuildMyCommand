package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.MessageLevel;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscordAdapterTest {
    @Test
    void textCommandDispatchMapsPrefixSourceAndPlatform() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success(
            ctx.source().id().orElseThrow() + ":" + ctx.commandInput().platform().id()
        )));
        DiscordAdapter adapter = DiscordAdapter.attach(framework);

        DiscordResponse response = adapter.execute(DiscordUser.of("42", "Ada"), DiscordTextCommand.of("!ping"));

        assertEquals(CommandResult.Status.SUCCESS, response.status());
        assertEquals(Optional.of("42:discord"), response.content());
        assertEquals(DiscordMessageVisibility.PUBLIC, response.visibility());
    }

    @Test
    void slashCommandSyncExportsArgumentsFlagsAliasesAndPermissions() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("give", command -> command
                .description("Give an item")
                .alias("grant")
                .permission("cmd.give")
                .argument("target", String.class)
                .argument("amount", Integer.class)
                .optionalArgument("ratio", Double.class)
                .flag("silent", "s")
                .option("force", boolean.class)
                .option("count", int.class)
                .option("big", Long.class)
                .option("rawBig", long.class)
                .option("decimal", double.class)
                .option("small", Float.class)
                .option("rawSmall", float.class)
                .executes(ctx -> Results.silent()));
        framework.registry()
            .command("bang", command -> command
                .alias("b")
                .suggestAliases(false)
                .executes(ctx -> Results.silent()));
        framework.registry()
            .command("say", command -> command
                .greedyArgument("note", String.class)
                .executes(ctx -> Results.silent()));
        framework.registry()
            .command("admin", admin -> admin
                .subcommand("hidden", hidden -> hidden
                    .hidden()
                    .executes(ctx -> Results.silent()))
                .subcommand("visible", visible -> visible
                    .executes(ctx -> Results.silent())));
        framework.registry()
            .command("secret", secret -> secret
                .hidden()
                .executes(ctx -> Results.silent()));
        DiscordAdapter adapter = DiscordAdapter.attach(framework);

        DiscordSlashCommand command = adapter.slashCommands().get(0);

        assertEquals("give", command.name());
        assertEquals("Give an item", command.description());
        assertEquals(List.of("grant"), command.aliases());
        assertEquals(Optional.of("cmd.give"), command.permission());
        assertEquals(List.of(
            new DiscordSlashOption("target", DiscordSlashOptionType.STRING, true, "Argument target"),
            new DiscordSlashOption("amount", DiscordSlashOptionType.INTEGER, true, "Argument amount"),
            new DiscordSlashOption("ratio", DiscordSlashOptionType.NUMBER, false, "Argument ratio"),
            new DiscordSlashOption("silent", DiscordSlashOptionType.BOOLEAN, false, "Alias -s"),
            new DiscordSlashOption("force", DiscordSlashOptionType.BOOLEAN, false, "Option force"),
            new DiscordSlashOption("count", DiscordSlashOptionType.INTEGER, false, "Option count"),
            new DiscordSlashOption("big", DiscordSlashOptionType.INTEGER, false, "Option big"),
            new DiscordSlashOption("rawBig", DiscordSlashOptionType.INTEGER, false, "Option rawBig"),
            new DiscordSlashOption("decimal", DiscordSlashOptionType.NUMBER, false, "Option decimal"),
            new DiscordSlashOption("small", DiscordSlashOptionType.NUMBER, false, "Option small"),
            new DiscordSlashOption("rawSmall", DiscordSlashOptionType.NUMBER, false, "Option rawSmall")
        ), command.options());
        assertEquals("bang", adapter.slashCommands().get(1).name());
        assertEquals(List.of(), adapter.slashCommands().get(1).aliases());
        assertEquals("say", adapter.slashCommands().get(2).name());
        assertEquals(new DiscordSlashOption("note", DiscordSlashOptionType.STRING, true, "Argument note"),
            adapter.slashCommands().get(2).options().get(0));
        assertEquals("admin", adapter.slashCommands().get(3).name());
        assertEquals(List.of("visible"), adapter.slashCommands().get(3).subcommands().stream()
            .map(DiscordSlashCommand::name)
            .toList());
    }

    @Test
    void rendererHonorsEphemeralMetadataAndPublicDefault() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("secret", command -> command.executes(ctx -> CommandResult.message(
                CommandResult.Status.SUCCESS,
                DiscordMessages.ephemeral("Hidden", MessageLevel.SUCCESS)
            )));
        framework.registry()
            .command("public", command -> command.executes(ctx -> Results.success("Visible")));
        DiscordAdapter adapter = DiscordAdapter.attach(framework);
        DiscordUser user = DiscordUser.of("42", "Ada");

        assertEquals(
            DiscordMessageVisibility.EPHEMERAL,
            adapter.execute(user, DiscordTextCommand.of("!secret")).visibility()
        );
        assertEquals(
            DiscordMessageVisibility.PUBLIC,
            adapter.execute(user, DiscordTextCommand.of("!public")).visibility()
        );
        assertEquals(new DiscordResponse(
            CommandResult.Status.FAILURE,
            Optional.of("Unknown command: missing"),
            DiscordMessageVisibility.PUBLIC
        ), adapter.execute(user, DiscordTextCommand.of("!missing")));
        assertEquals(DiscordResponse.silent(), new DiscordResponseRenderer().render(Results.silent()));
        assertEquals(DiscordResponse.silent(), new DiscordResponseRenderer().render(
            new CommandResult(CommandResult.Status.SUCCESS, Optional.empty())
        ));
        assertEquals(DiscordMessageVisibility.PUBLIC,
            new DiscordResponseRenderer().render(CommandResult.message(
                CommandResult.Status.SUCCESS,
                DiscordMessages.publicMessage("Shown", MessageLevel.SUCCESS)
            )).visibility());
    }

    @Test
    void sourcePermissionsDriveFrameworkPermissionChecks() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("admin", command -> command
                .permission("discord.admin")
                .executes(ctx -> Results.success("ok")));
        DiscordAdapter adapter = DiscordAdapter.attach(framework);

        DiscordResponse denied = adapter.execute(DiscordUser.of("1", "Guest"), DiscordTextCommand.of("!admin"));
        DiscordResponse allowed = adapter.execute(
            DiscordUser.of("2", "Mod", "discord.admin"),
            DiscordTextCommand.of("!admin")
        );

        assertEquals(CommandResult.Status.FAILURE, denied.status());
        assertEquals(Optional.of("Missing permission: discord.admin"), denied.content());
        assertEquals(CommandResult.Status.SUCCESS, allowed.status());
        assertEquals(Optional.of("ok"), allowed.content());
    }

    @Test
    void adapterPrefixInputMappingAndValidation() {
        CommandFramework framework = CommandFramework.create();
        DiscordAdapter adapter = DiscordAdapter.attach(framework).prefix("/");
        DiscordUser user = DiscordUser.of("42", "Ada");
        DiscordTextCommand clampedLow = new DiscordTextCommand("/ping", -10);
        DiscordTextCommand clampedHigh = new DiscordTextCommand("/ping", 99);

        assertEquals("/", adapter.prefix());
        assertEquals("ping", adapter.mapInput(user, clampedLow).normalizedInput());
        assertEquals(0, clampedLow.cursor());
        assertEquals(5, clampedHigh.cursor());
        assertEquals(adapter.config().capabilities(), adapter.capabilities());
        assertThrows(NullPointerException.class, () -> DiscordAdapter.attach(null));
        assertThrows(NullPointerException.class, () -> adapter.prefix(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.prefix(" "));
        assertThrows(NullPointerException.class, () -> new DiscordTextCommand(null, 0));
        assertThrows(NullPointerException.class, () -> adapter.mapInput(user, null));
    }

    @Test
    void commandSourceExposesDiscordUserRepliesMetadataAndNativeHandle() {
        Object nativeHandle = new Object();
        DiscordUser user = new DiscordUser("42", "Ada", Locale.FRANCE, Set.of("admin"), nativeHandle);
        DiscordCommandSource source = new DiscordCommandSource(user);
        CommandMessage reply = CommandMessage.success("hello");

        source.reply(reply);

        assertEquals(user, source.user());
        assertEquals(List.of(reply), source.replies());
        assertEquals(Optional.of("42"), source.id());
        assertEquals(Optional.of("Ada"), source.name());
        assertEquals(Locale.FRANCE, source.locale());
        assertEquals(Optional.of(nativeHandle), source.unwrap(Object.class));
        assertEquals(Optional.of(user), source.unwrap(DiscordUser.class));
        assertEquals(Optional.empty(), source.unwrap(String.class));
        assertEquals(Optional.of("42"), source.metadata("discord.userId"));
        assertEquals(Optional.of("Ada"), source.metadata("discord.username"));
        assertEquals(Optional.empty(), source.metadata("missing"));
        assertEquals(true, source.hasPermission("admin"));
        assertEquals(false, source.hasPermission("missing"));
        assertThrows(UnsupportedOperationException.class, () -> source.replies().add(reply));
        assertThrows(NullPointerException.class, () -> new DiscordCommandSource(null));
        assertThrows(NullPointerException.class, () -> source.unwrap(null));
        assertThrows(NullPointerException.class, () -> source.metadata(null));
        assertThrows(NullPointerException.class, () -> source.reply((CommandMessage) null));
        assertThrows(NullPointerException.class, () -> source.hasPermission(null));
    }

    @Test
    void discordRecordsValidateShape() {
        assertEquals(Locale.ROOT, DiscordUser.of("1", "Ada").locale());
        assertEquals(Set.of("a", "b"), DiscordUser.of("1", "Ada", "a", "b").permissions());
        assertThrows(NullPointerException.class, () -> new DiscordUser(null, "Ada", Locale.ROOT, Set.of(), null));
        assertThrows(NullPointerException.class, () -> new DiscordUser("1", null, Locale.ROOT, Set.of(), null));
        assertThrows(NullPointerException.class, () -> new DiscordUser("1", "Ada", Locale.ROOT, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DiscordUser(" ", "Ada", Locale.ROOT, Set.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new DiscordUser("1", "", Locale.ROOT, Set.of(), null));
        assertThrows(NullPointerException.class, () -> new DiscordResponse(null, Optional.empty(),
            DiscordMessageVisibility.PUBLIC));
        assertThrows(NullPointerException.class, () -> new DiscordResponse(CommandResult.Status.SUCCESS, null,
            DiscordMessageVisibility.PUBLIC));
        assertThrows(NullPointerException.class, () -> new DiscordResponse(CommandResult.Status.SUCCESS,
            Optional.empty(), null));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand(null, "desc", List.of(), List.of(),
            List.of(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand("name", null, List.of(), List.of(),
            List.of(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand("name", "desc", null, List.of(),
            List.of(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand("name", "desc", List.of(), null,
            List.of(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand("name", "desc", List.of(), List.of(),
            null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashCommand("name", "desc", List.of(), List.of(),
            List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new DiscordSlashCommand(" ", "desc", List.of(), List.of(),
            List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new DiscordSlashCommand("name", " ", List.of(), List.of(),
            List.of(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new DiscordSlashOption(null, DiscordSlashOptionType.STRING,
            true, "desc"));
        assertThrows(NullPointerException.class, () -> new DiscordSlashOption("name", null, true, "desc"));
        assertThrows(NullPointerException.class, () -> new DiscordSlashOption("name", DiscordSlashOptionType.STRING,
            true, null));
        assertThrows(IllegalArgumentException.class, () -> new DiscordSlashOption("", DiscordSlashOptionType.STRING,
            true, "desc"));
        assertThrows(IllegalArgumentException.class, () -> new DiscordSlashOption("name", DiscordSlashOptionType.STRING,
            true, ""));
    }
}
