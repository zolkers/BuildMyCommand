package dev.riege.buildmycommand.examples;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Flag;
import dev.riege.buildmycommand.annotation.Option;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Route;
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
        CommandResult punish(
            @Arg("target") String target,
            @Arg("reason") String reason,
            @Option("duration") Integer duration,
            @Flag("silent") boolean silent
        ) {
            int minutes = duration == null ? 60 : duration;
            String visibility = silent ? "silently" : "publicly";
            return Results.success("Punished " + target + " for " + minutes + "m " + visibility + ": " + reason);
        }
    }
}
