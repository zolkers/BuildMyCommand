package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class PlayerManagementBuilderExample {
    private PlayerManagementBuilderExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("player", player -> player
            .alias("p")
            .description("Player management root")
            .path("profile view", view -> view
                .description("View a player profile")
                .argument("target", String.class)
                .executes(ctx -> Results.success("Profile for " + ctx.arg("target", String.class))))
            .path("profile edit displayname", displayName -> displayName
                .argument("target", String.class)
                .greedyArgument("displayName", String.class)
                .executes(ctx -> Results.success(
                    "Renamed "
                        + ctx.arg("target", String.class)
                        + " to "
                        + ctx.arg("displayName", String.class))))
            .path("inventory clear", clear -> clear
                .permission("player.inventory.clear")
                .argument("target", String.class)
                .executes(ctx -> Results.success("Cleared inventory for " + ctx.arg("target", String.class))))
            .subcommand("moderation", moderation -> moderation
                .subcommand("warn", warn -> warn
                    .permission("player.moderation.warn")
                    .argument("target", String.class)
                    .greedyArgument("reason", String.class)
                    .executes(ctx -> Results.success(
                        "Warned "
                            + ctx.arg("target", String.class)
                            + ": "
                            + ctx.arg("reason", String.class))))
                .subcommand("mute", mute -> mute
                    .permission("player.moderation.mute")
                    .argument("target", String.class)
                    .option("minutes", Integer.class)
                    .greedyArgument("reason", String.class)
                    .executes(ctx -> Results.success(
                        "Muted "
                            + ctx.arg("target", String.class)
                            + " for "
                            + ctx.option("minutes", Integer.class).orElse(60)
                            + "m: "
                            + ctx.arg("reason", String.class))))
                .subcommand("ban", ban -> ban
                    .permission("player.moderation.ban")
                    .argument("target", String.class)
                    .flag("silent")
                    .greedyArgument("reason", String.class)
                    .executes(ctx -> Results.success(
                        "Banned "
                            + ctx.arg("target", String.class)
                            + " silent="
                            + ctx.flag("silent")
                            + ": "
                            + ctx.arg("reason", String.class))))
                .subcommand("history", history -> history
                    .argument("target", String.class)
                    .executes(ctx -> Results.success("Moderation history for " + ctx.arg("target", String.class))))));

        framework.registry()
            .route("player inventory give <target:String> <item:String> [amount:Integer] [--silent|-s]")
            .permission("player.inventory.give")
            .executes(ctx -> Results.success(
                "Gave "
                    + ctx.optionalArg("amount", Integer.class).orElse(1)
                    + " "
                    + ctx.arg("item", String.class)
                    + " to "
                    + ctx.arg("target", String.class)
                    + " silent="
                    + ctx.flag("silent")));

        framework.registry()
            .route("player economy balance add <target:String> <amount:Integer>")
            .permission("player.economy.add")
            .executes(ctx -> Results.success(
                "Added "
                    + ctx.arg("amount", Integer.class)
                    + " coins to "
                    + ctx.arg("target", String.class)));

        framework.registry()
            .route("player economy balance remove <target:String> <amount:Integer> [--reason:String|-r]")
            .permission("player.economy.remove")
            .executes(ctx -> Results.success(
                "Removed "
                    + ctx.arg("amount", Integer.class)
                    + " coins from "
                    + ctx.arg("target", String.class)
                    + ctx.option("reason", String.class).map(reason -> ": " + reason).orElse("")));

        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    private static CommandSource source() {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }
}
