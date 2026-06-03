package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationRouteExample {
    private AnnotationRouteExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
        return framework;
    }

    @CaseInsensitive(literals = true, options = true)
    static final class ModerationCommands {
        @Route("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
        @Description("Punish a user")
        @Permission("mod.punish")
        @Usage("/moderation punish <target> <reason> [--duration <minutes>] [--silent]")
        @Example("/mod punish Ada spam --duration 30 -s")
        CommandResult punish(@RouteCtx CommandContext route) {
            String target = route.arg("target", String.class);
            String reason = route.arg("reason", String.class);
            int minutes = route.option("duration", Integer.class).orElse(60);
            boolean silent = route.flag("silent");
            return Results.success("Punished " + target + " for " + minutes + "m silent=" + silent + ": " + reason);
        }
    }
}
