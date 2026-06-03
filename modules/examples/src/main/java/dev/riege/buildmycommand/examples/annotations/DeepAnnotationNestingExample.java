package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class DeepAnnotationNestingExample {
    private DeepAnnotationNestingExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new AdminCommands());
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    @Command("admin")
    @Alias("a")
    @CaseInsensitive(literals = true, options = true)
    @Description("Administrative command tree")
    static final class AdminCommands {
        @SubRoute("moderation|mod punish temporary|temp add <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
        @Permission("admin.moderation.punish.temp.add")
        CommandResult addTemporaryPunishment(@RouteCtx CommandContext route) {
            return Results.success(
                "Temporary punishment added for "
                    + route.arg("target", String.class)
                    + ": "
                    + route.arg("reason", String.class)
                    + " duration="
                    + route.option("duration", Integer.class).orElse(60)
                    + " silent="
                    + route.flag("silent")
            );
        }

        @SubRoute("moderation|mod punish temporary|temp remove <target:String>")
        @Permission("admin.moderation.punish.temp.remove")
        CommandResult removeTemporaryPunishment(@RouteCtx CommandContext route) {
            return Results.success("Temporary punishment removed for " + route.arg("target", String.class));
        }

        @SubRoute("moderation|mod punish temporary|temp list [target:String]")
        @Permission("admin.moderation.punish.temp.list")
        CommandResult listTemporaryPunishments(@RouteCtx CommandContext route) {
            return Results.success("Temporary punishments for "
                + route.optionalArg("target", String.class).orElse("everyone"));
        }

        @SubRoute("moderation|mod punish permanent|perm ban <target:String> <reason:String...>")
        @Permission("admin.moderation.punish.permanent.ban")
        CommandResult permanentBan(@RouteCtx CommandContext route) {
            return Results.success(
                "Permanent ban for "
                    + route.arg("target", String.class)
                    + ": "
                    + route.arg("reason", String.class)
            );
        }

        @SubRoute("moderation|mod punish permanent|perm unban <target:String>")
        @Permission("admin.moderation.punish.permanent.unban")
        CommandResult permanentUnban(@RouteCtx CommandContext route) {
            return Results.success("Permanent ban lifted for " + route.arg("target", String.class));
        }

        @SubRoute("moderation|mod appeal approve|accept <target:String>")
        @Permission("admin.moderation.appeal.approve")
        CommandResult approveAppeal(@RouteCtx CommandContext route) {
            return Results.success("Appeal approved for " + route.arg("target", String.class));
        }

        @SubRoute("moderation|mod appeal deny|reject <target:String> <reason:String...>")
        @Permission("admin.moderation.appeal.deny")
        CommandResult denyAppeal(@RouteCtx CommandContext route) {
            return Results.success(
                "Appeal denied for "
                    + route.arg("target", String.class)
                    + ": "
                    + route.arg("reason", String.class)
            );
        }

        @SubRoute("moderation|mod audit player|user <target:String>")
        @Permission("admin.moderation.audit.player")
        CommandResult auditPlayer(@RouteCtx CommandContext route) {
            return Results.success("Audit log for " + route.arg("target", String.class));
        }

        @SubRoute("moderation|mod audit staff|moderator <member:String>")
        @Permission("admin.moderation.audit.staff")
        CommandResult auditStaff(@RouteCtx CommandContext route) {
            return Results.success("Staff audit for " + route.arg("member", String.class));
        }
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
