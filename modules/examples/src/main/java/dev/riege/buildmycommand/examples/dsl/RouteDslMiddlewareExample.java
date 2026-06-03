package dev.riege.buildmycommand.examples.dsl;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Optional;

public final class RouteDslMiddlewareExample {
    private RouteDslMiddlewareExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
            .description("Punish a user")
            .permission("mod.punish")
            .middleware((context, command, path, next) -> {
                CommandResult result = next.proceed(context);
                return result.reply()
                    .map(reply -> new CommandResult(result.status(), Optional.of(
                        reply + " [" + String.join("/", path) + "]"
                    )))
                    .orElse(result);
            })
            .middleware((context, command, path, next) -> {
                if (!context.source().hasPermission("staff")) {
                    return Results.failure("Missing permission: staff");
                }
                return next.proceed(context);
            })
            .executes(ctx -> {
                String target = ctx.arg("target", String.class);
                String reason = ctx.arg("reason", String.class);
                int duration = ctx.option("duration", Integer.class).orElse(60);
                boolean silent = ctx.flag("silent");
                return Results.success("Punished " + target + " for " + duration + "m silent=" + silent + ": " + reason);
            });
        return framework;
    }

    public static CommandResult dispatch(CommandSource source, String input) {
        return create().dispatch(source, input);
    }
}
