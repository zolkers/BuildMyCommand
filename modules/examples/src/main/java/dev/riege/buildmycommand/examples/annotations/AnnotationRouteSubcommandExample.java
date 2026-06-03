package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationRouteSubcommandExample {
    private AnnotationRouteSubcommandExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new UserCommands());
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    @Command("user")
    @Alias("u")
    @CaseInsensitive(literals = true, options = true)
    static final class UserCommands {
        @SubRoute("rank set <target:String> <rank:String> [--silent|-s]")
        @Alias("roles put")
        @Permission("user.rank.set")
        CommandResult setRank(@RouteCtx CommandContext route) {
            String target = route.arg("target", String.class);
            String rank = route.arg("rank", String.class);
            boolean silent = route.flag("silent");
            return Results.success("Set " + target + " to " + rank + " silent=" + silent);
        }

        @SubRoute("note add <target:String> <message:String...> [--private|-p]")
        @Permission("user.note.add")
        CommandResult addNote(@RouteCtx CommandContext route) {
            String visibility = route.flag("private") ? "private" : "public";
            return Results.success(
                visibility + " note for "
                    + route.arg("target", String.class)
                    + ": "
                    + route.arg("message", String.class)
            );
        }

        @SubRoute("teleport <target:String> [world:String]")
        @Alias("tp")
        @Permission("user.teleport")
        CommandResult teleport(@RouteCtx CommandContext route) {
            return Results.success(
                "Teleporting "
                    + route.arg("target", String.class)
                    + " to "
                    + route.optionalArg("world", String.class).orElse("current world")
            );
        }
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
