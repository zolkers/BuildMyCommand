package dev.buildmycommand.examples;

import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;

public final class BuilderExample {
    private BuilderExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command
            .description("Reply with Pong")
            .executes(ctx -> Results.success("Pong")));
        return framework;
    }
}
