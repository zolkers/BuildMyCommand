package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class BuilderPathExample {
    private BuilderPathExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();

        framework.registry().command("user", user -> user
            .path("rank set promote", promote -> promote
                .description("Promote a user")
                .argument("target", String.class)
                .argument("rank", String.class)
                .executes(ctx -> Results.success(
                    "Promoted " + ctx.arg("target", String.class) + " to " + ctx.arg("rank", String.class))))
            .path("rank set demote", demote -> demote
                .argument("target", String.class)
                .executes(ctx -> Results.success("Demoted " + ctx.arg("target", String.class))))
            .path("profile open", profile -> profile
                .argument("target", String.class)
                .executes(ctx -> Results.success("Opening profile for " + ctx.arg("target", String.class)))));

        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
