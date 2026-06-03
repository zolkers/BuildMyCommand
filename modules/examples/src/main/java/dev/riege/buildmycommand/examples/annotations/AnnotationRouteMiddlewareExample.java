package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Optional;

public final class AnnotationRouteMiddlewareExample {
    private AnnotationRouteMiddlewareExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
        return framework;
    }

    public static CommandResult dispatch(CommandSource source, String input) {
        return create().dispatch(source, input);
    }

    @CaseInsensitive(literals = true, options = true)
    static final class ModerationCommands {
        @Route("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
        @Description("Punish a user")
        @Permission("mod.punish")
        @Usage("/moderation punish <target> <reason> [--duration <minutes>] [--silent]")
        @Example("/mod punish Ada spam --duration 30 -s")
        @Middleware({AuditMiddleware.class, StaffOnlyMiddleware.class})
        CommandResult punish(@RouteCtx CommandContext route) {
            String target = route.arg("target", String.class);
            String reason = route.arg("reason", String.class);
            int duration = route.option("duration", Integer.class).orElse(60);
            boolean silent = route.flag("silent");
            return Results.success("Punished " + target + " for " + duration + "m silent=" + silent + ": " + reason);
        }
    }

    public static final class AuditMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            CommandResult result = next.proceed(context);
            return result.reply()
                .map(reply -> new CommandResult(result.status(), Optional.of(
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
