package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;

public final class AnnotationMiddlewareExample {
    private AnnotationMiddlewareExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        dev.riege.buildmycommand.annotation.AnnotationCommandScanner.register(
            framework.registry(),
            new ModerationCommands()
        );
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(new CommandSource() {
        }, input);
    }

    @Command("moderation")
    @Middleware(AuditMiddleware.class)
    static final class ModerationCommands {
        @SubRoute("warn <target:String> <reason:String...>")
        @Middleware(StaffOnlyMiddleware.class)
        CommandResult warn(@RouteCtx CommandContext route) {
            return Results.success("Warned " + route.arg("target", String.class)
                + ": " + route.arg("reason", String.class));
        }

        @SubRoute("status")
        CommandResult status(@RouteCtx CommandContext route) {
            return Results.success("Moderation online");
        }
    }

    public static final class AuditMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            CommandResult result = next.proceed(context);
            return result.reply()
                .map(reply -> new CommandResult(result.status(), java.util.Optional.of(
                    reply + " [" + String.join("/", commandPath) + "]"
                )))
                .orElse(result);
        }
    }

    public static final class StaffOnlyMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            if (!context.source().hasPermission("staff")) {
                return Results.failure("Missing permission: staff");
            }
            return next.proceed(context);
        }
    }
}
