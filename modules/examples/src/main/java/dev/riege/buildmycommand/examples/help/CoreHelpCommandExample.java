/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.help;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionSet;
import dev.riege.buildmycommand.annotation.help.AnnotatedCommandHelp;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.riege.buildmycommand.core.help.CommandHelp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class CoreHelpCommandExample {
    private CoreHelpCommandExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();

        AnnotationCommandScanner.register(framework.registry(), new ProfileCommands());
        AnnotationCommandScanner.register(framework.registry(), new PartyCommands());
        AnnotationCommandScanner.register(framework.registry(), new AdminCommands());
        AnnotationCommandScanner.register(framework.registry(), new StaffCommands());
        AnnotationCommandScanner.register(framework.registry(), new InternalCommands());
        AnnotationCommandScanner.register(framework.registry(), new AnnotatedCommandHelp(CommandHelp.forFramework(framework)));
        return framework;
    }

    public static CommandResult dispatch(CommandSource source, String input) {
        return create().dispatch(source, input);
    }

    public static List<String> suggest(CommandSource source, String input, int cursor) {
        return create().suggest(source, input, cursor);
    }

    @Command("profile")
    @CommandGroup("Players")
    @CaseInsensitive(literals = true, options = true)
    static final class ProfileCommands {
        @SubRoute("view <target:String>")
        @Description("Open a player's profile card")
        @Usage("/profile view <player>")
        @Example("/profile view Ada")
        CommandResult view(@RouteCtx CommandContext route) {
            return Results.success("Profile for " + route.arg("target", String.class));
        }

        @SubRoute("message <target:String> <message:String...> [--silent|-s]")
        @Description("Send a private profile note")
        @Usage("/profile message <player> <message> [--silent]")
        @Example("/profile message Ada Welcome back -s")
        CommandResult message(@RouteCtx CommandContext route) {
            return Results.success(
                "Message to "
                    + route.arg("target", String.class)
                    + " silent="
                    + route.flag("silent")
                    + ": "
                    + route.arg("message", String.class)
            );
        }

        @Suggest("target")
        SuggestionSet onlinePlayers(SuggestionContext context) {
            return onlinePlayersFromContext(context);
        }
    }

    @Command("party")
    @CommandGroup("Social")
    @CaseInsensitive(literals = true, options = true)
    static final class PartyCommands {
        @SubRoute("invite <target:String>")
        @Description("Invite a player to your party")
        @Usage("/party invite <player>")
        @Example("/party invite Alex")
        CommandResult invite(@RouteCtx CommandContext route) {
            return Results.success("Invite sent to " + route.arg("target", String.class));
        }

        @Suggest("target")
        SuggestionSet onlinePlayers(SuggestionContext context) {
            return onlinePlayersFromContext(context);
        }
    }

    @Command("admin")
    @CommandGroup("Administration")
    @CaseInsensitive(literals = true, options = true)
    static final class AdminCommands {
        @SubRoute("reload [area:String]")
        @Description("Reload a server subsystem")
        @Permission("admin.reload")
        @Usage("/admin reload [area]")
        @Example("/admin reload commands")
        CommandResult reload(@RouteCtx CommandContext route) {
            return Results.success("Reloaded " + route.optionalArg("area", String.class).orElse("all"));
        }

        @SubRoute("audit player <target:String>")
        @Description("Read player audit information")
        @Permission(value = "admin\\.audit\\..*", regex = true)
        @Usage("/admin audit player <player>")
        @Example("/admin audit player Ada")
        CommandResult auditPlayer(@RouteCtx CommandContext route) {
            return Results.success("Audit for " + route.arg("target", String.class));
        }

        @Suggest("area")
        SuggestionSet reloadAreas(SuggestionContext context) {
            return SuggestionSet.of("commands", "permissions", "cache").filteringCurrentToken();
        }

        @Suggest("target")
        SuggestionSet onlinePlayers(SuggestionContext context) {
            return onlinePlayersFromContext(context);
        }
    }

    @Command("staff")
    @CommandGroup("Staff")
    @CaseInsensitive(literals = true, options = true)
    static final class StaffCommands {
        @SubRoute("notes list")
        @Description("List staff notes")
        @Require("staff.notes")
        @Usage("/staff notes list")
        @Example("/staff notes list")
        CommandResult listNotes(@RouteCtx CommandContext route) {
            return Results.success("Staff notes");
        }
    }

    @Command("internal")
    @Hidden
    @CaseInsensitive(literals = true, options = true)
    static final class InternalCommands {
        @SubRoute("diagnostics dump")
        @Description("Hidden internal support command")
        CommandResult diagnostics(@RouteCtx CommandContext route) {
            return Results.success("diagnostics");
        }
    }

    private static SuggestionSet onlinePlayersFromContext(SuggestionContext context) {
        return SuggestionSet.of("Ada", "Alex", "Linus", "Grace").filteringCurrentToken();
    }

    public static final class ExampleSource implements CommandSource {
        private final String name;
        private final Set<String> permissions;

        public ExampleSource(String name, Set<String> permissions) {
            this.name = name;
            this.permissions = Set.copyOf(permissions);
        }

        @Override
        public Optional<String> name() {
            return Optional.of(name);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permission == null || permission.isBlank() || permissions.contains(permission);
        }

        @Override
        public boolean hasPermissionMatching(Pattern pattern) {
            return permissions.stream().anyMatch(permission -> pattern.matcher(permission).matches());
        }
    }
}
