package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.MessageLevel;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                .flag("silent", "s")
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
            new DiscordSlashOption("silent", DiscordSlashOptionType.BOOLEAN, false, "Alias -s")
        ), command.options());
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
}
