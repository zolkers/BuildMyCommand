package dev.riege.buildmycommand.examples.lifecycle;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class MiddlewareAndErrorsExample {
    private MiddlewareAndErrorsExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .middleware((context, command, path, next) -> {
                long start = System.nanoTime();
                CommandResult result = next.proceed(context);
                context.source().reply("Command " + String.join(" ", path) + " took " + (System.nanoTime() - start) + "ns");
                return result;
            })
            .errorHandler((context, command, path, error) -> Results.failure("Command failed: " + error.getMessage()))
            .build();
        framework.registry().command("explode", command -> command.executes(ctx -> {
            throw new IllegalStateException("boom");
        }));
        return framework;
    }
}
