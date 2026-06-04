/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

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
            .subRoute("profile view <target:String>", view -> view
                .description("View a player profile")
                .executes(ctx -> Results.success("Profile for " + ctx.arg("target", String.class))))
            .subRoute("profile edit displayname <target:String> <displayName:String...>", displayName -> displayName
                .executes(ctx -> Results.success(
                    "Renamed "
                        + ctx.arg("target", String.class)
                        + " to "
                        + ctx.arg("displayName", String.class))))
            .subRoute("inventory clear <target:String>", clear -> clear
                .permission("player.inventory.clear")
                .executes(ctx -> Results.success("Cleared inventory for " + ctx.arg("target", String.class))))
            .subRoute("moderation warn <target:String> <reason:String...>", warn -> warn
                .permission("player.moderation.warn")
                .executes(ctx -> Results.success(
                    "Warned "
                        + ctx.arg("target", String.class)
                        + ": "
                        + ctx.arg("reason", String.class))))
            .subRoute("moderation mute <target:String> <reason:String...> [--minutes:Integer|-m]", mute -> mute
                .permission("player.moderation.mute")
                .executes(ctx -> Results.success(
                    "Muted "
                        + ctx.arg("target", String.class)
                        + " for "
                        + ctx.option("minutes", Integer.class).orElse(60)
                        + "m: "
                        + ctx.arg("reason", String.class))))
            .subRoute("moderation ban <target:String> <reason:String...> [--silent|-s]", ban -> ban
                .permission("player.moderation.ban")
                .executes(ctx -> Results.success(
                    "Banned "
                        + ctx.arg("target", String.class)
                        + " silent="
                        + ctx.flag("silent")
                        + ": "
                        + ctx.arg("reason", String.class))))
            .subRoute("moderation history <target:String>", history -> history
                .executes(ctx -> Results.success("Moderation history for " + ctx.arg("target", String.class))))
            .subRoute("inventory give <target:String> <item:String> [amount:Integer] [--silent|-s]", give -> give
                .permission("player.inventory.give")
                .executes(ctx -> Results.success(
                    "Gave "
                        + ctx.optionalArg("amount", Integer.class).orElse(1)
                        + " "
                        + ctx.arg("item", String.class)
                        + " to "
                        + ctx.arg("target", String.class)
                        + " silent="
                        + ctx.flag("silent"))))
            .subRoute("economy balance add <target:String> <amount:Integer>", add -> add
                .permission("player.economy.add")
                .executes(ctx -> Results.success(
                    "Added "
                        + ctx.arg("amount", Integer.class)
                        + " coins to "
                        + ctx.arg("target", String.class))))
            .subRoute("economy balance remove <target:String> <amount:Integer> [--reason:String|-r]", remove -> remove
                .permission("player.economy.remove")
                .executes(ctx -> Results.success(
                    "Removed "
                        + ctx.arg("amount", Integer.class)
                        + " coins from "
                        + ctx.arg("target", String.class)
                        + ctx.option("reason", String.class).map(reason -> ": " + reason).orElse("")))));

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
