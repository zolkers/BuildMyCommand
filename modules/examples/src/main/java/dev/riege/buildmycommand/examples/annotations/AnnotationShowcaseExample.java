/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.SuggestAliases;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionSet;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandFramework;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class AnnotationShowcaseExample {
    private static final String PLAYERS_METADATA = "players";

    private AnnotationShowcaseExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new ShowcaseCommands());
        AnnotationCommandScanner.register(framework.registry(), new StandaloneRouteCommands());
        return framework;
    }

    public static CommandResult dispatch(CommandSource source, String input) {
        return create().dispatch(source, input);
    }

    public static List<String> suggest(CommandSource source, String input, int cursor) {
        return create().suggest(source, input, cursor);
    }

    public static CommandSource source(String... permissions) {
        return source(List.of("Ada", "Alex", "Grace", "Linus"), permissions);
    }

    public static CommandSource source(List<String> onlinePlayers, String... permissions) {
        return new CommandSource() {
            private final Set<String> permissionSet = Set.of(permissions);

            @Override
            public Optional<String> name() {
                return Optional.of("Ada");
            }

            @Override
            public Optional<Object> metadata(String key) {
                Objects.requireNonNull(key, "key");
                return PLAYERS_METADATA.equals(key) ? Optional.of(onlinePlayers) : Optional.empty();
            }

            @Override
            public boolean hasPermission(String permission) {
                return permission == null || permission.isBlank() || permissionSet.contains(permission);
            }

            @Override
            public Set<String> permissions() {
                return permissionSet;
            }
        };
    }

    @Command("showcase")
    @Alias({"sc", "demo"})
    @CaseInsensitive(literals = true, options = true)
    @SuggestAliases(false)
    @CommandGroup("Showcase")
    @Description("Annotation API showcase")
    @Usage("/showcase <route>")
    @Example({"/showcase profile Ada", "/sc mod punish Ada spam -d 30 -s"})
    @Middleware(TraceMiddleware.class)
    static final class ShowcaseCommands {
        @SubRoute("profile|p <target:String> [viewer:String]")
        @Description("Open a profile with an optional viewer override")
        @Usage("/showcase profile <target> [viewer]")
        @Example("/showcase profile Ada Alex")
        @CommandGroup("Players")
        @SuggestAliases(false)
        CommandResult profile(@RouteCtx CommandContext route) {
            return Results.success("Profile target="
                + route.arg("target", String.class)
                + " viewer="
                + route.optionalArg("viewer", String.class).orElse("self"));
        }

        @SubRoute("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
        @Description("Punish a player with options, aliases, middleware, permission, and requirement metadata")
        @Permission("showcase.moderation.punish")
        @Require("staff || owner")
        @Usage("/showcase moderation punish <target> <reason> [--duration <minutes>] [--silent]")
        @Example("/showcase mod punish Ada spam --duration 30 -s")
        @Middleware(StaffAuditMiddleware.class)
        @CommandGroup("Moderation")
        CommandResult punish(@RouteCtx CommandContext route) {
            int duration = route.option("duration", Integer.class).orElse(60);
            return Results.success("Punished "
                + route.arg("target", String.class)
                + " for "
                + duration
                + "m silent="
                + route.flag("silent")
                + ": "
                + route.arg("reason", String.class));
        }

        @SubRoute("audit <target:String> [--format:String]")
        @Description("Read audit data protected by a regex permission")
        @Permission(value = "showcase\\.audit\\..*", regex = true)
        @Usage("/showcase audit <target> [--format json]")
        @Example("/showcase audit Ada --format json")
        @CommandGroup("Audit")
        CommandResult audit(@RouteCtx CommandContext route) {
            return Results.success("Audit "
                + route.arg("target", String.class)
                + " format="
                + route.option("format", String.class).orElse("text"));
        }

        @SubRoute("diagnostics dump")
        @Description("Hidden support command with a cooldown")
        @Usage("/showcase diagnostics dump")
        @Hidden
        @Cooldown(value = 5, unit = TimeUnit.SECONDS)
        @CommandGroup("Internal")
        CommandResult diagnostics(@RouteCtx CommandContext route) {
            return Results.success("Diagnostics at " + Instant.EPOCH);
        }

        @Subcommand("version")
        @Description("Minimal @Subcommand method style for tiny leaves")
        @CommandGroup("System")
        CommandResult version(CommandContext context) {
            return Results.success("showcase version");
        }

        @Subcommand("legacy")
        @Description("Nested @Subcommand groups are supported, but route DSL is preferred for deep paths")
        static final class LegacyGroup {
            @Subcommand("status")
            @Description("Nested @Subcommand leaf")
            CommandResult status() {
                return Results.success("legacy status");
            }
        }

        @Suggest("target")
        SuggestionSet onlinePlayers(SuggestionContext context) {
            return context.sourceMetadata(PLAYERS_METADATA)
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(SuggestionSet::of)
                .map(set -> set.filteringCurrentToken().tooltip("online player").priority(20))
                .orElseGet(SuggestionSet::empty);
        }

        @Suggest("viewer")
        List<String> viewerNames() {
            return List.of("self", "console", "staff");
        }

        @Suggest("format")
        List<String> auditFormats() {
            return List.of("text", "json", "compact");
        }

        @Suggest("reason")
        SuggestionSet reasons() {
            return SuggestionSet.rich(List.of(
                rich("spam", "common moderation reason", 10),
                rich("griefing", "world damage", 8),
                rich("toxicity", "chat moderation", 6)
            )).filteringCurrentToken();
        }

        private static Suggestion rich(String value, String tooltip, int priority) {
            return new Suggestion(value, Optional.of(tooltip), 0, 0, SuggestionType.ARGUMENT, priority);
        }
    }

    static final class StandaloneRouteCommands {
        @Command("about")
        @Description("Tiny method-level @Command root")
        CommandResult about() {
            return Results.success("BuildMyCommand annotation showcase");
        }

        @Route("utility echo <message:String...>")
        @Description("Standalone @Route method outside a @Command class")
        @Usage("/utility echo <message>")
        @Example("/utility echo hello")
        CommandResult echo(@RouteCtx CommandContext route) {
            return Results.success(route.arg("message", String.class));
        }
    }

    public static final class TraceMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }

    public static final class StaffAuditMiddleware implements CommandMiddleware {
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
}
