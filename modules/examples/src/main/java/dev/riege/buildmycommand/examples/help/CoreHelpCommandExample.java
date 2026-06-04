/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.help;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.PermissionSpec;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionSet;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        AnnotationCommandScanner.register(framework.registry(), new HelpCommands(framework));
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

    @CommandGroup("System")
    @CaseInsensitive(literals = true, options = true)
    static final class HelpCommands {
        private final CommandFramework framework;

        private HelpCommands(CommandFramework framework) {
            this.framework = framework;
        }

        @Route("help [query:String...]")
        @Alias("h")
        @Description("Show visible commands or inspect one command")
        @Usage("/help [command]")
        @Example({"/help", "/help profile message"})
        CommandResult help(@RouteCtx CommandContext route) {
            String query = route.optionalArg("query", String.class)
                .map(String::trim)
                .orElse("");
            if (!query.isBlank()) {
                String details = framework.help(route.source(), query);
                return Results.success(header("Command Help") + "\n" + details);
            }

            List<HelpEntry> entries = visibleEntries(framework.graph(), route.source());
            Map<String, List<HelpEntry>> byGroup = entries.stream()
                .sorted(Comparator.comparing(HelpEntry::group).thenComparing(HelpEntry::path))
                .collect(Collectors.groupingBy(
                    HelpEntry::group,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

            StringBuilder builder = new StringBuilder(header("Command Help"));
            byGroup.forEach((group, groupEntries) -> {
                builder.append("\n\n").append(group);
                for (HelpEntry entry : groupEntries) {
                    builder.append("\n  ")
                        .append(entry.path())
                        .append(" - ")
                        .append(entry.description());
                    if (!entry.aliases().isEmpty()) {
                        builder.append(" (aliases: ").append(String.join(", ", entry.aliases())).append(")");
                    }
                }
            });
            builder.append("\n\nUse /help <command> for usage, examples, arguments, options, and permissions.");
            return Results.success(builder.toString());
        }

        @Suggest("query")
        SuggestionSet commands(SuggestionContext context) {
            List<String> paths = visibleEntries(framework.graph(), context.source()).stream()
                .map(HelpEntry::path)
                .toList();
            return SuggestionSet.of(paths).filteringCurrentToken();
        }
    }

    private static SuggestionSet onlinePlayersFromContext(SuggestionContext context) {
        return SuggestionSet.of("Ada", "Alex", "Linus", "Grace").filteringCurrentToken();
    }

    private static List<HelpEntry> visibleEntries(CommandGraph graph, CommandSource source) {
        List<HelpEntry> entries = new ArrayList<>();
        for (CommandNode root : graph.roots()) {
            collectVisibleEntries(source, List.of(root), new ArrayList<>(), entries);
        }
        return entries;
    }

    private static void collectVisibleEntries(
        CommandSource source,
        List<CommandNode> lineage,
        List<String> path,
        List<HelpEntry> entries
    ) {
        CommandNode node = lineage.get(lineage.size() - 1);
        if (node.metadata().hidden() || denied(source, lineage).isPresent()) {
            return;
        }

        List<String> currentPath = new ArrayList<>(path);
        currentPath.add(node.literal());
        if (node.executor().isPresent()) {
            entries.add(new HelpEntry(
                String.join(" ", currentPath),
                node.description().orElse("No description"),
                node.metadata().group().orElse("Other"),
                node.aliases()
            ));
        }

        for (CommandNode child : node.children()) {
            List<CommandNode> childLineage = new ArrayList<>(lineage);
            childLineage.add(child);
            collectVisibleEntries(source, childLineage, currentPath, entries);
        }
    }

    private static Optional<String> denied(CommandSource source, List<CommandNode> lineage) {
        for (CommandNode node : lineage) {
            Optional<PermissionSpec> permission = node.permissionSpec();
            if (permission.isPresent() && !hasPermission(source, permission.get())) {
                return Optional.of(permission.get().display());
            }
            Optional<String> requirement = node.metadata().requirement();
            if (requirement.isPresent() && !source.hasPermission(requirement.get())) {
                return requirement;
            }
        }
        return Optional.empty();
    }

    private static boolean hasPermission(CommandSource source, PermissionSpec permission) {
        if (permission.regex()) {
            return source.hasPermissionMatching(Pattern.compile(permission.value()));
        }
        return source.hasPermission(permission.value());
    }

    private static String header(String title) {
        return "== " + title + " ==";
    }

    private record HelpEntry(String path, String description, String group, List<String> aliases) {
        private HelpEntry {
            aliases = List.copyOf(aliases);
        }
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
