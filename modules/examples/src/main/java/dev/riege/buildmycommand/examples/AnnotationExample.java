package dev.riege.buildmycommand.examples;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Permission;
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
        @Command("kick")
        @Description("Kick a user")
        @Permission("mod.kick")
        CommandResult kick(@Arg("target") String target) {
            return Results.success("Kicked " + target);
        }
    }
}
