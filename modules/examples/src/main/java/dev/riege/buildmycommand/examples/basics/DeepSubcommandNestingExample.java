package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class DeepSubcommandNestingExample {
    private DeepSubcommandNestingExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("admin", admin -> admin
            .alias("a")
            .description("Administrative command tree")
            .subcommand("moderation", moderation -> moderation
                .alias("mod")
                .description("Moderation tools")
                .subcommand("punish", punish -> punish
                    .description("Punishment workflows")
                    .subcommand("temporary", temporary -> temporary
                        .alias("temp")
                        .subcommand("add", add -> add
                            .permission("admin.moderation.punish.temp.add")
                            .argument("target", String.class)
                            .greedyArgument("reason", String.class)
                            .option("duration", Integer.class)
                            .flag("silent")
                            .executes(ctx -> Results.success(
                                "Temporary punishment added for "
                                    + ctx.arg("target", String.class)
                                    + ": "
                                    + ctx.arg("reason", String.class)
                                    + " duration="
                                    + ctx.option("duration", Integer.class).orElse(60)
                                    + " silent="
                                    + ctx.flag("silent"))))
                        .subcommand("remove", remove -> remove
                            .permission("admin.moderation.punish.temp.remove")
                            .argument("target", String.class)
                            .executes(ctx -> Results.success(
                                "Temporary punishment removed for " + ctx.arg("target", String.class))))
                        .subcommand("list", list -> list
                            .permission("admin.moderation.punish.temp.list")
                            .optionalArgument("target", String.class)
                            .executes(ctx -> Results.success(
                                "Temporary punishments for " + ctx.argOr("target", "everyone")))))
                    .subcommand("permanent", permanent -> permanent
                        .alias("perm")
                        .subcommand("ban", ban -> ban
                            .permission("admin.moderation.punish.permanent.ban")
                            .argument("target", String.class)
                            .greedyArgument("reason", String.class)
                            .executes(ctx -> Results.success(
                                "Permanent ban for "
                                    + ctx.arg("target", String.class)
                                    + ": "
                                    + ctx.arg("reason", String.class))))
                        .subcommand("unban", unban -> unban
                            .permission("admin.moderation.punish.permanent.unban")
                            .argument("target", String.class)
                            .executes(ctx -> Results.success(
                                "Permanent ban lifted for " + ctx.arg("target", String.class))))))
                .subcommand("appeal", appeal -> appeal
                    .description("Appeal review workflow")
                    .subcommand("approve", approve -> approve
                        .permission("admin.moderation.appeal.approve")
                        .argument("target", String.class)
                        .executes(ctx -> Results.success("Appeal approved for " + ctx.arg("target", String.class))))
                    .subcommand("deny", deny -> deny
                        .permission("admin.moderation.appeal.deny")
                        .argument("target", String.class)
                        .greedyArgument("reason", String.class)
                        .executes(ctx -> Results.success(
                            "Appeal denied for "
                                + ctx.arg("target", String.class)
                                + ": "
                                + ctx.arg("reason", String.class)))))
                .subcommand("audit", audit -> audit
                    .description("Moderation audit log")
                    .subcommand("player", player -> player
                        .argument("target", String.class)
                        .executes(ctx -> Results.success("Audit log for " + ctx.arg("target", String.class))))
                    .subcommand("staff", staff -> staff
                        .argument("member", String.class)
                        .executes(ctx -> Results.success("Staff audit for " + ctx.arg("member", String.class)))))));

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
