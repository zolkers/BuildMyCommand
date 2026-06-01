package dev.buildmycommand.examples;

import dev.buildmycommand.annotation.AnnotationCommandScanner;
import dev.buildmycommand.annotation.Arg;
import dev.buildmycommand.annotation.Command;
import dev.buildmycommand.annotation.Description;
import dev.buildmycommand.annotation.Permission;
import dev.buildmycommand.api.CommandResult;
import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;

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
