package dev.riege.buildmycommand.examples;

import dev.riege.buildmycommand.annotation.*;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationExample {
    private AnnotationExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
        return framework;
    }

    static final class ModerationCommands {
        @Route("moderation punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
        @Description("Punish a user")
        @Permission("mod.punish")
        CommandResult punish(@RouteCtx dev.riege.buildmycommand.api.CommandContext route) {
            String target = route.arg("target", String.class);
            String reason = route.arg("reason", String.class);
            int minutes = route.option("duration", Integer.class).orElse(60);
            boolean silent = route.flag("silent");
            String visibility = silent ? "silently" : "publicly";
            return Results.success("Punished " + target + " for " + minutes + "m " + visibility + ": " + reason);
        }
    }
}
